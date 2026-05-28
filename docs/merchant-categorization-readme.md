# Merchant Normalization & Transaction Categorization System

## Overview

This document explains how raw financial transaction descriptions are parsed, normalized, matched to canonical merchants, and categorized in the Finance Manager application.

Raw transaction data from banks and card networks is messy. The same merchant may appear as `TIMS #4021 ON`, `TIM HORTONS TORONTO`, or `TIMHORTONS 88KQ21`. The system solves this through a **progressive resolution pipeline** — cheap operations run first, and expensive ones only fire when cheaper ones fail.

---

## High-Level Workflow

```
Raw Transaction
      |
      ▼
┌─────────────────────────┐
│ 1. Transaction Import   │  Raw data from bank / card network
└───────────┬─────────────┘
            |
            ▼
┌─────────────────────────┐
│ 2. Field Parsing        │  Extract merchant name, amount, date
└───────────┬─────────────┘
            |
            ▼
┌─────────────────────────┐
│ 3. Normalization        │  Strip noise, standardize casing,
│    (MerchantNormalizer) │  expand abbreviations
└───────────┬─────────────┘
            |
            ▼
┌─────────────────────────┐
│ 4. Merchant Resolution  │  Tier 1: Exact match against aliases
│    (ResolutionPipeline) │  Tier 2: Fuzzy match with similarity scoring
└───────────┬─────────────┘
            |
            ▼
┌─────────────────────────┐
│ 5. Categorization       │  Map resolved merchant to spending category
│    (IntelligenceEngine) │  Fallback to regex category rules
└───────────┬─────────────┘
            |
            ▼
┌─────────────────────────┐
│ 6. Loyalty & Analytics  │  Compute merchant loyalty scores,
│                         │  spending trends, insights
└─────────────────────────┘
```

The full pipeline is orchestrated by `AnalyticsRefreshOrchestrator` and runs on a scheduled cron job (`0 30 2 * * ?` — 2:30 AM daily) or on-demand via `POST /api/analytics/refresh`. Each refresh records resolution quality metrics (Tier 1/2 hit rates, promotion count, top unresolved descriptors) in the refresh run metadata.

---

## 1. Transaction Import & Field Parsing

When transactions are imported from a bank or card network, each record contains fields like:
- `merchant` or `name` — the raw merchant description string
- `amount` — transaction value
- `date` — when the transaction occurred

The `TransactionFieldParser` applies typed fields to ensure dates and amounts are properly converted before entering the pipeline.

---

## 2. Normalization (`MerchantNormalizer`)

Before any matching can occur, raw merchant strings are transformed into a clean, canonical form. Poor normalization causes false misses in exact matching and degrades fuzzy match quality.

### Normalization Steps (in order)

1. **Uppercase and trim** — Eliminates case-sensitivity issues entirely.
2. **Apply regex normalization rules** — Ordered list of compiled patterns that strip noise (including brand-family pre-expansions like `TIMS` → `TIM HORTONS`).
3. **Expand abbreviations** — Replace known short forms with full words.
4. **Normalize whitespace** — Collapse multiple spaces/tabs into single spaces.
5. **Brand canonicalization** (`BrandCanonicalizer`) — Match against `merchant_brand_rules` patterns to collapse variants (e.g. `AMZN MKTP CA` → `AMAZON`, `TIM HORTONS #2387` → `TIM HORTONS`).

All merchant keys and alias lookups use **uppercase** normalized strings end-to-end.

### Normalization Rules (from `analytics/normalization-rules.yml`)

Rules are stored in `normalization_rules` (PostgreSQL) and loaded at startup. They are applied in priority order (lowest priority number first).

| Rule | Pattern | What It Strips |
|------|---------|----------------|
| Strip domain suffixes | `\.(COM\|CA\|NET\|ORG)` | `.com`, `.ca`, etc. |
| Strip common merchant suffixes | `\s+(BILL\|ONLINE\|PURCHASE\|PMT\|PAYMENT\|SUBSCRIPTION\|RECURRING)\s*$` | Transaction type noise |
| Strip trailing city names | `\s+(TORONTO\|VANCOUVER\|CALGARY\|...)` | Geographic suffixes |
| Strip terminal reference codes | `\s+(?=[A-Z0-9]*[0-9])(?=[A-Z0-9]*[A-Z])[A-Z0-9]{5,}$` | Terminal IDs like `8X92KQ` (requires both letters and digits; does not strip words like `HORTONS`) |
| Expand TIMS / TIMHORTONS | `^TIMS\b`, `^TIMHORTONS\b` | Expands abbreviations to `TIM HORTONS` before noise stripping |
| Strip store numbers | `\s*#\d+` | Store numbers like `#1021` |
| Strip geographic suffixes | `\s+(ON\|BC\|AB\|QC\|CA\|USA)\s*$` | Province/state/country codes |
| Strip phone numbers | `\s+\d{3}[-.]?\d{3}[-.]?\d{4}$` | Embedded phone numbers |
| Strip special characters | `[*/\\]` | Punctuation noise |
| Strip trailing 3+ digit numbers | `\s+\d{3,}$` | Large numeric suffixes |
| Strip numeric store IDs | `\s+\d+$` | Trailing store IDs |

### Example

```
Raw:      "TIM HORTONS #4021 TORONTO ON 8X92KQ"
Step 1:   "TIM HORTONS #4021 TORONTO ON 8X92KQ"  (uppercase)
Step 2:   "TIM HORTONS #4021 TORONTO ON"         (strip terminal code)
Step 3:   "TIM HORTONS TORONTO ON"               (strip store number)
Step 4:   "TIM HORTONS TORONTO"                  (strip geographic suffix)
Step 5:   "TIM HORTONS"                          (strip city name)
Step 6:   "TIM HORTONS"                          (brand canonicalization — no change)
Final:    "TIM HORTONS"
```

```
Raw:      "AMZN MKTP CA*PY8IM93N3"
Final:    "AMAZON"   (noise stripped, then ^amzn brand rule)
```

### Abbreviation Expansions (from `analytics/abbreviation-mappings.yml`)

Applied **after** regex rules. Common mappings include:

| Abbreviation | Expansion |
|--------------|-----------|
| `PMT` | `PAYMENT` |
| `PRCHSE` | `PURCHASE` |
| `INSUR` | `INSURANCE` |
| `XFER` | `TRANSFER` |
| `MKT` / `MKTPL` / `MKTPLCE` | `MARKET` / `MARKETPLACE` |
| `CA` | `CANADA` |
| `INC` | `INCORPORATED` |
| `LTD` | `LIMITED` |
| `BLVD` | `BOULEVARD` |
| `ST` / `AVE` / `RD` / `DR` / `HWY` | `STREET` / `AVENUE` / `ROAD` / `DRIVE` / `HIGHWAY` |

Rules and abbreviations live in the database and can be reloaded at runtime via `merchantNormalizer.reloadRules()` without restarting the application.

---

## 3. Merchant Resolution Pipeline (`MerchantResolutionPipeline`)

After normalization, the system attempts to identify the merchant through a two-tier matching strategy.

### Data Model

#### `merchants` table
- `id` (UUID) — primary key
- `canonical_name` — the clean, official name (e.g., "Tim Hortons")
- `category` — spending category string (e.g., "Dining")
- `subcategory` — finer classification (e.g., "Coffee & Fast Food")
- `mcc_code` — ISO 18245 Merchant Category Code

#### `merchant_aliases` table
- `id` — primary key
- `merchant_id` — FK to `merchants`
- `normalized_name` — the cleaned string that maps to this merchant (unique, indexed)
- `source` — how the alias was created: `seed`, `tier2_promoted`, `manual`, `user_feedback`
- `confidence` — confidence score (0.000 to 1.000)

### Tier 1 — Exact Match (`TierOneMerchantResolver`)

The normalized string is looked up directly in `merchant_aliases` via a hash index on `normalized_name`.

- **Speed:** O(1) lookup — effectively free compute
- **Cache:** Results are cached with Caffeine (`merchantAliasCache`) keyed by the **raw** description, so repeated raw strings hit memory directly
- **Confidence:** 1.0 (perfect match)
- **Expected coverage:** Resolves the majority of transactions because real-world volume is dominated by a small set of recurring merchants

```java
@Cacheable(value = "merchantAliasCache", key = "#rawDescription")
public Optional<ResolvedMerchant> resolve(String rawDescription) {
    String normalized = normalizer.normalize(rawDescription);
    return aliasRepository.findByNormalizedName(normalized)
        .map(alias -> new ResolvedMerchant(..., ResolutionTier.EXACT_MATCH, 1.0, ...));
}
```

### Tier 2 — Fuzzy Match (`TierTwoMerchantResolver`)

When Tier 1 misses, the system computes similarity scores against candidate merchants.

#### Candidate Retrieval — PostgreSQL Trigram Index

PostgreSQL's `pg_trgm` extension builds a GIN trigram index over `normalized_name`, allowing fast pre-filtering to the top 10 most similar aliases without a full table scan.

```sql
SELECT ma.*
FROM merchant_aliases ma
JOIN merchants m ON ma.merchant_id = m.id
WHERE ma.normalized_name % :input          -- trigram similarity operator
ORDER BY similarity(ma.normalized_name, :input) DESC
LIMIT 10;
```

#### Similarity Scoring — Jaro-Winkler + Token Sort Ratio

A weighted combination of two metrics:

| Metric | Weight | Purpose |
|--------|--------|---------|
| Jaro-Winkler | 0.40 | Handles character transpositions; prefix-weighted |
| Token Sort Ratio | 0.60 | Neutralizes word-order variation (`HORTONS TIM` vs `TIM HORTONS` scores 1.0) |

**Formula:**
```
final_score = (jaro_winkler * 0.40) + (token_sort_ratio * 0.60)
```

#### Threshold

- Default confidence threshold: **0.82** (configurable via `merchant.tier2.confidence-threshold`)
- Matches below this threshold are discarded (can be queued for manual review in future iterations)

---

## 4. Self-Learning: Alias Promotion

Every high-confidence Tier 2 match is **automatically promoted** to a Tier 1 alias.

```
Tier 2 fuzzy hit (confidence >= 0.82)
         |
         ▼
  Save new row in merchant_aliases
  source = "tier2_promoted"
  normalized_name = the input that just matched
         |
         ▼
  Evict cache entry so next occurrence hits Tier 1
```

This means the system **learns continuously without ML model training**. Over time, the alias table grows and Tier 2 fires less frequently, keeping the pipeline fast.

### Why This Works

- Most people shop at the same merchants repeatedly
- Once `TIM HORTONS #4021 TORONTO ON` is seen and fuzzy-matched, the next occurrence resolves instantly
- The `source` column tracks provenance for audit and debugging

---

## 5. Brand Rules (`analytics/merchant-brand-rules.yml`)

At application startup, `MerchantDataSeedService` reads `merchant-brand-rules.yml` and seeds the `merchant_brand_rules`, `merchants`, and `merchant_aliases` tables. Rules are also applied at **runtime** by `BrandCanonicalizer` after base normalization.

Brand rules map merchant name **patterns** to **canonical brand names** for grouping:

| Pattern | Canonical |
|---------|-----------|
| `^amazon` / `^amzn` | `AMAZON` |
| `^walmart` / `^wm\s` | `walmart` |
| `^apple` / `^itunes` | `apple` |
| `^google` / `^youtube` | `google` |
| `^microsoft` / `^msft` | `microsoft` |
| `^tim hortons` / `^tims` / `^timhortons` | `TIM HORTONS` |
| `^metro` | `metro` |
| `^sobeys` | `sobeys` |
| `^loblaws` | `loblaws` |
| `^shoppers drug mart` | `shoppers drug mart` |

Patterns use case-insensitive regex (`(?i)`). They are matched in priority order, and **first match wins**.

These rules provide the initial merchant master list. After seeding, the system grows this list organically through Tier 2 alias promotion.

---

## 6. Categorization (`MerchantIntelligenceEngine`)

Once a merchant is resolved, the engine maps it to a spending category.

### Resolution-Driven Categorization

If the two-tier pipeline resolves a merchant, the category string from the `merchants` table is converted to a slug and looked up in `spending_categories`:

```java
// e.g., "Dining" -> "dining"
categoryRepository.findBySlug("dining").map(SpendingCategory::getId)
```

### Fallback: Regex Category Rules (`merchant_category_rules`)

If merchant resolution fails entirely, the system falls back to regex rules applied against `merchantRaw + " " + merchantKey`:

```java
for (CompiledCategoryRule rule : rules) {
    if (rule.pattern().matcher(searchText).find()) {
        return rule.categoryId();
    }
}
```

Rules are stored in the `merchant_category_rules` table with:
- `pattern` — Java regex string
- `category_id` — FK to `spending_categories`
- `priority` — evaluation order (lowest first)

If no rule matches, the transaction is marked as **uncategorized**.

### Spending Categories (`spending_categories`)

| Field | Description |
|-------|-------------|
| `slug` | URL-safe identifier (e.g., `groceries`, `dining`) |
| `display_name` | Human-readable label (e.g., "Groceries") |
| `parent_id` | Optional hierarchy support |

---

## 7. Loyalty & Analytics (`MerchantLoyaltyService`)

After categorization, the system computes merchant loyalty metrics:

### Metrics Calculated Per Merchant

| Metric | Description |
|--------|-------------|
| `total_transactions` | How many times you've shopped there |
| `total_spend` | Cumulative spend |
| `avg_transaction_size` | Average transaction value |
| `first_visit` / `last_visit` | Date range of activity |
| `visit_frequency_days` | Average days between visits |
| `loyalty_score` | 0-100 composite score |
| `spend_growth_rate` | First-half vs second-half spend change |

### Loyalty Score Formula (0-100)

| Factor | Weight | Logic |
|--------|--------|-------|
| Frequency | 40% | More transactions = higher score (capped at 20) |
| Total Spend | 30% | Logarithmic scale to avoid extreme values |
| Consistency | 20% | Weekly visits ideal; penalty for longer gaps |
| Recency | 10% | More recent visits score higher |

### API Endpoint

```
GET /api/merchant-loyalty?categoryId={id}&limit={n}
POST /api/merchant-loyalty/calculate
```

---

## 8. Key Source Files

| File | Role |
|------|------|
| `MerchantNormalizer.java` | Applies regex rules + abbreviation expansion + brand canonicalization |
| `BrandCanonicalizer.java` | Applies brand-family regex rules from `merchant_brand_rules` |
| `MerchantResolutionMetrics.java` | Tracks Tier 1/2 hit rates, promotions, and unresolved descriptors per refresh |
| `MerchantAnalyticsRebuildService.java` | Uppercases legacy data, merges duplicate aliases, deduplicates loyalty keys |
| `TierOneMerchantResolver.java` | Exact alias lookup with Caffeine caching |
| `TierTwoMerchantResolver.java` | Fuzzy matching via trigram candidates + Jaro-Winkler scoring |
| `MerchantResolutionPipeline.java` | Orchestrates Tier 1 -> Tier 2 -> alias promotion |
| `MerchantIntelligenceEngine.java` | Categorization: resolution first, regex fallback |
| `TransactionNormalizationService.java` | Converts raw `Transaction` -> `NormalizedTransaction` |
| `MerchantDataSeedService.java` | Bootstraps rules, abbreviations, merchants, and aliases from YAML |
| `MerchantLoyaltyService.java` | Computes loyalty metrics and scores |
| `AnalyticsRefreshOrchestrator.java` | Runs the full pipeline end-to-end |
| `AnalyticsRefreshJob.java` | Scheduled and on-demand refresh trigger |

### Key YAML Configuration Files

| File | Role |
|------|------|
| `analytics/normalization-rules.yml` | Regex patterns for cleaning merchant strings |
| `analytics/abbreviation-mappings.yml` | Short-form -> full-word expansions |
| `analytics/merchant-brand-rules.yml` | Initial merchant patterns and canonical names |

### Key Database Tables

| Table | Role |
|-------|------|
| `merchants` | Canonical merchant master list |
| `merchant_aliases` | Normalized string -> merchant mappings (lookup table) |
| `normalization_rules` | Regex rules (live-editable, reloadable) |
| `abbreviation_mappings` | Word expansions (live-editable, reloadable) |
| `merchant_category_rules` | Fallback regex-based category classification |
| `spending_categories` | Category taxonomy (slug, display_name, parent_id) |
| `normalized_transactions` | Clean, enriched transaction records |
| `merchant_loyalty_metrics` | Computed merchant loyalty scores |
| `analytics_refresh_runs` | Audit log of pipeline executions |

---

## 9. How to Extend

### Adding a New Normalization Rule

1. Insert into `normalization_rules` (PostgreSQL):
   ```sql
   INSERT INTO normalization_rules (rule_name, pattern, replacement, priority, enabled)
   VALUES ('Strip new suffix', '\s+NEWSUFFIX\s*$', '', 25, true);
   ```
2. Call `merchantNormalizer.reloadRules()` or restart the app.
3. Optionally add to `analytics/normalization-rules.yml` for future seeds.

### Adding a New Merchant Alias

```sql
INSERT INTO merchant_aliases (merchant_id, normalized_name, source, confidence)
VALUES ('merchant-uuid-here', 'CLEANED MERCHANT NAME', 'manual', 1.000);
```

### Adding a Category Rule

```sql
INSERT INTO merchant_category_rules (pattern, category_id, priority)
VALUES ('(?i)some.*pattern', 5, 100);
```

---

## 10. Limitations

This two-tier design will **not** reliably resolve:

- **Completely unrecognized merchants** with no alias or trigram similarity (e.g., a brand new local business)
- **Merchants described only by location or address** rather than a business name
- **Non-English merchant names** where abbreviation and noise patterns differ significantly
- **Intentionally obfuscated descriptions** used by some subscription services

These cases are candidates for future Tier 3 (embedding-based semantic search) and Tier 4 (LLM fallback) approaches.

---

## 11. Technology Stack

| Concern | Technology |
|---------|------------|
| Application Framework | Spring Boot |
| Database | PostgreSQL (with `pg_trgm` extension) |
| ORM | Spring Data JPA |
| Cache | Caffeine (in-process) |
| String Utilities | Apache Commons Lang 3 |
| Similarity Scoring | Apache Commons Text (Jaro-Winkler) |
| Scheduling | Spring `@Scheduled` + `@Async` |
