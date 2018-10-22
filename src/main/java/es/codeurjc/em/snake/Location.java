package es.codeurjc.em.snake;

enum Direction {
	NONE, NORTH, SOUTH, EAST, WEST
}

public class Location {

	public static final int PLAYFIELD_WIDTH = 640;
	public static final int PLAYFIELD_HEIGHT = 480;
	public static final int GRID_SIZE = 10;

	public int x;
	public int y;

	public Location(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public Location getAdjacentLocation(Direction direction) {
		
		switch (direction) {
		case NORTH:
			return new Location(this.x, this.y - Location.GRID_SIZE);
		case SOUTH:
			return new Location(this.x, this.y + Location.GRID_SIZE);
		case EAST:
			return new Location(this.x + Location.GRID_SIZE, this.y);
		case WEST:
			return new Location(this.x - Location.GRID_SIZE, this.y);
		case NONE:
			// fall through
		default:
			return this;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + x;
		result = prime * result + y;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Location other = (Location) obj;
		if (x != other.x)
			return false;
		if (y != other.y)
			return false;
		return true;
	}
}
