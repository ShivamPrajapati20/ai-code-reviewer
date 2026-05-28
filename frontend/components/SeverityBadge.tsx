import {
  AlertTriangle,
  ShieldAlert,
  Sparkles,
  type LucideIcon,
} from "lucide-react";

type Props = {
  severity: string;
};

export default function SeverityBadge({ severity }: Props) {
  const styles: Record<string, { className: string; icon: LucideIcon }> = {
    critical: {
      className: "bg-rose-100 text-rose-700 ring-rose-200",
      icon: ShieldAlert,
    },
    warning: {
      className: "bg-amber-100 text-amber-800 ring-amber-200",
      icon: AlertTriangle,
    },
    suggestion: {
      className: "bg-sky-100 text-sky-700 ring-sky-200",
      icon: Sparkles,
    },
  };

  const style =
    styles[severity] || {
      className: "bg-slate-100 text-slate-700 ring-slate-200",
      icon: Sparkles,
    };
  const Icon = style.icon;

  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-bold capitalize ring-1 ${style.className}`}
    >
      <Icon className="h-3.5 w-3.5" />
      {severity}
    </span>
  );
}
