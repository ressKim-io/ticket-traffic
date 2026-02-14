export type GameStatus = "SCHEDULED" | "OPEN" | "SOLD_OUT" | "CLOSED";
export type SeatGrade = "VIP" | "RED" | "BLUE" | "GRAY";

export interface GameResponse {
  id: number;
  stadiumName: string;
  homeTeam: string;
  awayTeam: string;
  gameDate: string;
  ticketOpenAt: string;
  status: GameStatus;
  maxTicketsPerUser: number;
}

export interface GameDetailResponse {
  id: number;
  stadium: StadiumSummary;
  homeTeam: string;
  awayTeam: string;
  gameDate: string;
  ticketOpenAt: string;
  status: GameStatus;
  maxTicketsPerUser: number;
  sections: SectionSeatSummary[];
}

export interface StadiumSummary {
  id: number;
  name: string;
  address: string;
}

export interface SectionSeatSummary {
  sectionId: number;
  sectionName: string;
  grade: SeatGrade;
  totalSeats: number;
  availableSeats: number;
}

export interface GameListParams {
  status?: GameStatus;
  teamName?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}
