package cava;

import java.util.Hashtable;

public class AlbumContainer {
	Hashtable<Integer, Integer> hashRowNumToID;
	Album[] albums=null;
	TrackDatabase db;
	int numAlbums;
	
	public AlbumContainer() {
		db = new TrackDatabase();
		if(db.isConnected()){
			hashRowNumToID = new Hashtable<Integer, Integer>();
		}
	}
	
	public void updateArtistID(int artistID){
		//hashRowNumToID.clear();
		hashRowNumToID = new Hashtable<Integer, Integer>();
		albums = null;
		setAlbums(artistID);
	}
	
	public void getAlbumsWithIDs(Integer[] albumIDs){
		hashRowNumToID = new Hashtable<Integer, Integer>();
		albums = db.getAlbumsWithIDs(albumIDs);
		if(albums!=null){
			int i = 0;
			for(Album album: albums){
				hashRowNumToID.put(album.getAlbumID(),i++);
			}
			numAlbums = i;
		}else{
			numAlbums = 0;
		}
	}
	
	private void setAlbums(int artistID){
		if(artistID==0){
			albums = db.getAllAlbums();
		}else{
			albums = db.getAlbumsByArtist(artistID);
		}
		if(albums==null){
			numAlbums = 0;
			return;
		}
		int i = 0;
		for(Album album: albums){
			hashRowNumToID.put(album.getAlbumID(),i++);
		}
		numAlbums = i;//i starts at 0, so offsets last addition
	}
	
	public int getNumAlbums(){
		return this.numAlbums;
	}
	
	public Album getAlbumByRowNumber(int row){
		try{
			return albums[row];
		}catch (Exception e) {
			return null;
		}
	}
	
	public Album getAlbumByID(int albumID){
		Integer row = hashRowNumToID.get(albumID);
		if(row==null){
			return null;
		}else{
			try{
				return albums[row];
			}catch (Exception e) {
				return null;
			}
		}
	}
	
	public int getRowNumFromID(int albumID){
		Integer row =  hashRowNumToID.get(albumID);
		if(row==null){
			return -1;
		}else{
			return row;
		}
	}
	
	public int getFirstRowMatching(String query){
		return getFirstRowMatching(query, 0, albums.length);
	}
	
	public int getFirstRowMatching(String query,int start, int end){
		if(start >= end){
			return -1;
		}
		int mid = Math.round((end+start)/2);
		if(mid==start){
			return -1;
		}
		if(albums[mid].getAlbumName().toLowerCase().startsWith(query)){
			//Loop until it doesn't start with the query string
			while(mid >= 0 && albums[mid].getAlbumName().toLowerCase().startsWith(query)){
				mid--;
			}
			//plus one more to ignore all, plus one to get first matching row
			return mid+2;
		}else{
			if(albums[mid].getAlbumName().toLowerCase().compareTo(query) < 0){
				//System.out.println("Compared to:" + artists[mid].getArtistName().substring(0, query.length()));
				return getFirstRowMatching(query, mid, end);
			}else{
				return getFirstRowMatching(query,start,mid);
			}
		}
		
	}
	
}
