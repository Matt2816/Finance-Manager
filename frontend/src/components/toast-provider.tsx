"use client";

import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
} from "react";
import { CheckCircle, XCircle, X } from "lucide-react";

export type ToastType = "success" | "error";

export interface Toast {
  id: string;
  message: string;
  type: ToastType;
}

interface ToastContextValue {
  showToast: (message: string, type?: ToastType) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error("useToast must be used within ToastProvider");
  return ctx;
}

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const timeoutIds = useRef<Set<ReturnType<typeof setTimeout>>>(new Set());

  const showToast = useCallback((message: string, type: ToastType = "success") => {
    const id = `${Date.now()}-${Math.random()}`;
    setToasts((prev) => [...prev, { id, message, type }]);
    const timeoutId = setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
      timeoutIds.current.delete(timeoutId);
    }, 4000);
    timeoutIds.current.add(timeoutId);
  }, []);

  useEffect(() => {
    const currentTimeoutIds = timeoutIds.current;

    return () => {
      currentTimeoutIds.forEach(clearTimeout);
      currentTimeoutIds.clear();
    };
  }, []);

  const remove = (id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  };

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      <div className="fixed bottom-0 left-0 right-0 z-[100] flex flex-col items-center gap-2 p-4 sm:bottom-6 sm:right-6 sm:left-auto sm:items-end sm:w-auto pointer-events-none">
        {toasts.map((toast) => (
          <div
            key={toast.id}
            role={toast.type === "error" ? "alert" : "status"}
            aria-live={toast.type === "error" ? "assertive" : "polite"}
            className={`pointer-events-auto flex items-center gap-3 rounded-xl px-4 py-3 shadow-lg text-sm font-medium min-w-[280px] max-w-[90vw] sm:max-w-md transform transition-all duration-300 translate-y-0 opacity-100 ${
              toast.type === "success"
                ? "bg-green-600 text-white"
                : "bg-red-600 text-white"
            }`}
          >
            {toast.type === "success" ? (
              <CheckCircle className="h-5 w-5 shrink-0" />
            ) : (
              <XCircle className="h-5 w-5 shrink-0" />
            )}
            <span className="flex-1 break-words">{toast.message}</span>
            <button
              onClick={() => remove(toast.id)}
              className="shrink-0 opacity-80 hover:opacity-100"
              aria-label="Dismiss"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}
