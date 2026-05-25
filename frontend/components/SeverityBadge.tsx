// Displays colored badge based on severity level
// Critical = red, Warning = yellow, Suggestion = blue
type Props = {
  severity: string;
};

export default function SeverityBadge({ severity }: Props) {
  const styles: Record<string, string> = {
    critical:
      "bg-red-100 text-red-700 border border-red-300",
    warning:
      "bg-yellow-100 text-yellow-700 border border-yellow-300",
    suggestion:
      "bg-blue-100 text-blue-700 border border-blue-300",
  };

  const icons: Record<string, string> = {
    critical: "🔴",
    warning: "🟡",
    suggestion: "🔵",
  };

  const style =
    styles[severity] ||
    "bg-gray-100 text-gray-700 border border-gray-300";

  return (
    <span
      className={`px-2 py-1 rounded-full text-xs 
        font-semibold ${style}`}
    >
      {icons[severity]} {severity.toUpperCase()}
    </span>
  );
}