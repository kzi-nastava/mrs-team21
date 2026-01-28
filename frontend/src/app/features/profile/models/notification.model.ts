export interface UserNotification {
  id: number;
  userId: number;
  rideId?: number | null;
  type: string;
  message: string;
  createdAt: string;
  readAt?: string | null;
}
