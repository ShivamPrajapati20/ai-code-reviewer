import { FileCode2, Wrench } from "lucide-react";
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
    <article className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm transition hover:-translate-y-0.5 hover:border-slate-300 hover:shadow-lg">
      <div className="mb-3 flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div className="flex flex-wrap items-center gap-2">
          <SeverityBadge severity={comment.severity} />
          <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-bold capitalize text-slate-600">
            {comment.category}
          </span>
        </div>

        <span className="inline-flex items-center gap-1.5 break-all rounded-xl bg-slate-950 px-3 py-2 font-mono text-xs font-semibold text-slate-100">
          <FileCode2 className="h-3.5 w-3.5 shrink-0" />
          {comment.fileName}
          {comment.lineNumber && `: ${comment.lineNumber}`}
        </span>
      </div>

      <p className="text-sm font-black leading-6 text-slate-950">
        {comment.issue}
      </p>

      <div className="mt-3 rounded-xl border border-emerald-100 bg-emerald-50 px-4 py-3">
        <p className="mb-1 inline-flex items-center gap-1.5 text-xs font-black uppercase tracking-[0.18em] text-emerald-700">
          <Wrench className="h-3.5 w-3.5" />
          Suggested fix
        </p>
        <p className="text-sm leading-6 text-emerald-950">{comment.fix}</p>
      </div>
    </article>
  );
}
