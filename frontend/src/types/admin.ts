export interface DashboardResponse {
  totalBookings: number;
  confirmedBookings: number;
  totalRevenue: number;
  totalGames: number;
}

export interface GameStatsResponse {
  gameId: number;
  homeTeam: string | null;
  awayTeam: string | null;
  totalBookings: number;
  confirmedBookings: number;
  cancelledBookings: number;
  totalRevenue: number;
  totalRefunds: number;
  updatedAt: string;
}
