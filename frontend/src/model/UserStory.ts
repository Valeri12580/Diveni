interface UserStory {
  id: number | null;
  title: string;
  key: string | null;
  url: string | null ;
  description: string;
  estimation: string | null;
  isActive: boolean;
}

export default UserStory;
