package es.codeurjc.em.snake;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

public class Snake {
  //https://help.github.com/articles/fetching-a-remote/ 
	private static final int DEFAULT_LENGTH = 5;

	private final int id;

	private Location head;
	private final Deque<Location> tail = new ArrayDeque<>();
	private int length = DEFAULT_LENGTH;

	private final String hexColor;
	private String nombre_jugador;
	private Direction direction;
	private Sala sala;
	private int puntuacion = 0;
	private Thread hiloBlock;
	private final WebSocketSession session;
	
	//atributos de la serpiente
	public Snake(int id, WebSocketSession session,String nombre) {
		this.nombre_jugador=nombre;
		this.id = id;
		this.session = session;
		this.hexColor = SnakeUtils.getRandomHexColor();
		
		resetState();
	}
	//restaura los valores de la serpiente (todo a 0)
	private void resetState() {
		this.direction = Direction.NONE;
		this.head = SnakeUtils.getRandomLocation();
		this.tail.clear();
		this.length = DEFAULT_LENGTH;
	}
	//restaura los valores cuando muere la serpiente
	private synchronized void kill() throws Exception {
		resetState();
		sendMessage("{\"type\": \"dead\"}");
	}
	//cada vez que "matas" a una serpiente tu tamaño aumenta
	private synchronized void reward() throws Exception {
		this.length++;
		sendMessage("{\"type\": \"kill\"}");
	}
	
	//envio de mensajes
	synchronized protected void sendMessage(String msg)  {
		try{
		if(this.session.isOpen())
		this.session.sendMessage(new TextMessage(msg));
		}catch(Exception e){
			
		}
	}
	
	//movimiento serpiente
	public synchronized void update(Collection<Snake> snakes) throws Exception {

		Location nextLocation = this.head.getAdjacentLocation(this.direction);

		if (nextLocation.x >= Location.PLAYFIELD_WIDTH) {
			nextLocation.x = 0;
		}
		if (nextLocation.y >= Location.PLAYFIELD_HEIGHT) {
			nextLocation.y = 0;
		}
		if (nextLocation.x < 0) {
			nextLocation.x = Location.PLAYFIELD_WIDTH;
		}
		if (nextLocation.y < 0) {
			nextLocation.y = Location.PLAYFIELD_HEIGHT;
		}

		if (this.direction != Direction.NONE) {
			this.tail.addFirst(this.head);
			if (this.tail.size() > this.length) {
				this.tail.removeLast();
			}
			this.head = nextLocation;
		}

		handleCollisions(snakes);
	}
	
	//consulta de colisiones
	private void handleCollisions(Collection<Snake> snakes) throws Exception {

		for (Snake snake : snakes) {
			//se chocan las cabezas 
			boolean headCollision = this.id != snake.id && snake.getHead().equals(this.head);
			//choca con la cola
			boolean tailCollision = snake.getTail().contains(this.head);
			if(snake!=null)
			if(snake.getSala()!=null)	
			if(snake.getSala().getComida()!=null){
			boolean comidaCollision =snake.getHead().equals(snake.getSala().getComida().getL());
			
			if(comidaCollision){			
				snake.puntuacion++;
				System.out.println("-------------->puntuacion:"+snake.getPuntuacion()+" serpiente:"+snake.getName());
				snake.reward();
				snake.getSala().setComida(null);
			}
			}
			if (headCollision || tailCollision) {		
				kill();
				if (this.id != snake.id) {
					this.puntuacion--;
					snake.puntuacion++;
					snake.reward();
				}
			}
			
		}
	}
	
	//Getters y Setters
	
	//posicion de la cabeza
	public synchronized Location getHead() {
		return this.head;
	}
	//posicion de la cola
	public synchronized Collection<Location> getTail() {
		return this.tail;
	}
	//cambia la direccion de la serpiente
	public synchronized void setDirection(Direction direction) {
		this.direction = direction;
	}
	//devuelve 
	public int getId() {
		return this.id;
	}

	public String getHexColor() {
		return this.hexColor;
	}
	
	public void setName(String name){
		this.nombre_jugador=name;
	}
	public String getName(){
		return this.nombre_jugador;
	}
	public void setSala(Sala s){
		this.sala=s;
	}
	public Sala getSala(){
		return this.sala;
	}
	public WebSocketSession getsession(){
		
		return this.session;
	}
	public int getPuntuacion(){
		return this.puntuacion;
	}
	public int getLenght(){
		return this.length;
	}
	
	public Thread getHiloBlock() {
		return hiloBlock;
	}
	public void setHiloBlock(Thread hiloBlock) {
		this.hiloBlock = hiloBlock;
	}
	}

