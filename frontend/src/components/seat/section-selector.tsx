import type { SectionSeatSummary } from "@/types";

const gradeColors: Record<string, string> = {
  VIP: "border-yellow-400 bg-yellow-50 text-yellow-800",
  R: "border-red-400 bg-red-50 text-red-800",
  S: "border-blue-400 bg-blue-50 text-blue-800",
  A: "border-green-400 bg-green-50 text-green-800",
  B: "border-gray-400 bg-gray-50 text-gray-800",
};

const gradeColorSelected: Record<string, string> = {
  VIP: "border-yellow-500 bg-yellow-200 ring-2 ring-yellow-400",
  R: "border-red-500 bg-red-200 ring-2 ring-red-400",
  S: "border-blue-500 bg-blue-200 ring-2 ring-blue-400",
  A: "border-green-500 bg-green-200 ring-2 ring-green-400",
  B: "border-gray-500 bg-gray-200 ring-2 ring-gray-400",
};

interface SectionSelectorProps {
  sections: SectionSeatSummary[];
  selectedId: number | null;
  onSelect: (sectionId: number) => void;
}

export function SectionSelector({
  sections,
  selectedId,
  onSelect,
}: SectionSelectorProps) {
  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
      {sections.map((section) => {
        const isSelected = section.sectionId === selectedId;
        const grade = section.grade;
        const colorClass = isSelected
          ? gradeColorSelected[grade] ?? gradeColorSelected.B
          : gradeColors[grade] ?? gradeColors.B;

        return (
          <button
            key={section.sectionId}
            onClick={() => onSelect(section.sectionId)}
            disabled={section.availableSeats === 0}
            aria-pressed={isSelected}
            className={`rounded-lg border-2 p-3 text-left transition-all disabled:cursor-not-allowed disabled:opacity-40 ${colorClass}`}
          >
            <p className="text-sm font-semibold">{section.sectionName}</p>
            <p className="mt-1 text-xs">
              {section.grade} &middot; {section.availableSeats}/
              {section.totalSeats}
            </p>
          </button>
        );
      })}
    </div>
  );
}
