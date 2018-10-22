package es.codeurjc.em.snake;

public class Comida {

	private Location l;

	public Comida(Location Loc){
		this.l=Loc;
	}
	
	public Location getL() {
		return l;
	}

	public void setL(Location l) {
		this.l = l;
	}
	
}
