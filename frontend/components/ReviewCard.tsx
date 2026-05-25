import SeverityBadge from "./SeverityBadge";

type Comment = {
  id: string;
  severity: string;
  category: string;
  lineNumber: number | null;
  issue: string;
  fix: string;
  fileName: string;
};

type Props = {
  comment: Comment;
};

export default function ReviewCard({ comment }: Props) {
  return (
    <div className="border rounded-lg p-3 sm:p-4 
      mb-3 bg-white shadow-sm 
      hover:shadow-md transition">

      <div className="flex flex-col sm:flex-row 
        sm:items-center sm:justify-between 
        gap-2 mb-2">

        <SeverityBadge severity={comment.severity} />

        <span className="text-xs text-gray-500 
          font-mono break-all">
          {comment.fileName}
          {comment.lineNumber &&
            ` — Line ${comment.lineNumber}`}
        </span>
      </div>

      <span className="text-xs bg-gray-100 
        text-gray-600 px-2 py-0.5 rounded mb-2 
        inline-block capitalize">
        {comment.category}
      </span>

      <p className="text-sm font-medium 
        text-gray-800 mt-2">
        ⚠️ {comment.issue}
      </p>

      <p className="text-sm text-green-700 mt-1">
        ✅ <span className="font-medium">Fix:</span>{" "}
        {comment.fix}
      </p>
    </div>
  );
}