export interface SupportMessage {
  id: number;
  senderId: number;
  senderName: string;
  senderSurname: string;
  receiverId: number;
  receiverName: string;
  receiverSurname: string;
  content: string;
  createdAt: string;
}

export interface SupportConversationSummary {
  userId: number;
  userName: string;
  userSurname: string;
  lastMessage: string;
  lastMessageAt: string;
  lastSenderId: number;
  lastSenderRole: 'PASSENGER' | 'DRIVER' | 'ADMIN';
}
