package es.codeurjc.em.snake;

import java.awt.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Collection.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Random;

public class SnakeGame {

	private long TICK_DELAY = 100;
	private long[] ticks = { 200, 150, 100 };

	private ConcurrentHashMap<Integer, Snake> snakes = new ConcurrentHashMap<>();
	private ConcurrentHashMap<Integer, Snake> snakesMuro = new ConcurrentHashMap<>();
	private ConcurrentHashMap<Integer, Sala> salas = new ConcurrentHashMap<>();
	private SalaChat chatGlobal = new SalaChat();

	private AtomicInteger numSnakes = new AtomicInteger();
	private volatile AtomicInteger numSalas = new AtomicInteger();

	private ScheduledExecutorService scheduler;
	public AtomicInteger snakeIds = new AtomicInteger(0);
	public AtomicInteger salasIds = new AtomicInteger(0);

	private Lock l = new ReentrantLock();
	// private Lock snakeLock = new ReentrantLock();

	// Añade una serpiente a snakes y snakes muro
	public void addSnake(Snake snake) {
		snakes.put(snake.getId(), snake);
		snakesMuro.put(snake.getId(), snake);
		numSnakes.getAndIncrement();
	}

	// Añade una serpiente a la sala de chat
	public void addSnakeChat(Snake snake) {
		chatGlobal.AñadirJugador(snake);
	}

	// Añade una sala al mapa de salas
	public boolean addSala(Sala sala) {
		salas.put(sala.getId(), sala);
		numSalas.getAndIncrement();

		return true;
	}

	// Comprueba si la sala existe
	public boolean comprobarSala(String sala) {

		for (Sala sal : salas.values()) {
			String key = sal.getName();
			if (sala.equals(key)) {
				return true;
			}

		}
		return false;
	}

	//Elimina una serpiente de una sala concreta
	public void removeSnake(Snake snake) throws Exception {

		Sala sal = snake.getSala();
		numSnakes.getAndDecrement();
		snakeIds.getAndDecrement();
		// snakeLock.lock();
		sal.EliminarJugador(snake);
		// snakeLock.unlock();
		if (sal.getLista().size() == 1) {
			String mg = String.format("{\"type\": \"fin\"}");
			Snake sn = null;
			for(Snake s : sal.getLista().values()){
				sn = s;
			}
			sn.sendMessage(mg);
			sal.getLista().clear();
			numSnakes.getAndDecrement();
			removeSala(sal);
		}else if (sal.getLista().size() <= 0){
			sal.getLista().clear();
			numSnakes.getAndDecrement();
			removeSala(sal);
		}

	}
	
	//Elimina una sala
	void removeSala(Sala sala) throws Exception {

		if (numSnakes.get() <= 0) {

			resetServer();
		} else {
			sala.getLista().clear();
			salas.remove(sala.getId());
			for (Sala s : salas.values()){
				System.out.println(s.getId());
			}
		}
	}
	
	//Manejas las actualizaciones de cada "frame" del juego
	//Comida, ganador, nuevas posiciones, etc
	private void tick() {

		for (Sala sal : salas.values()) {
			try {

				if (sal.partida_empezada == true) {

					if ((sal.getComida() == null) && (sal.getContadorComida() <= 3)) {
						generarComida(sal);
						sal.setContadorComida(sal.getContadorComida() + 1);

					}
					// comprobar!!!
					if (sal.getContadorComida() > 3 || sal.logSerp()) {
						// l.lock();
						String mg = String.format("{\"type\": \"fin\"}");
						broadcast(mg, sal);
						sal.partida_empezada = false;
						// l.unlock();
					} else {

						if (!sal.getLista().isEmpty()) {
							for (Snake snake : sal.getLista().values()) {
								snake.update(sal.getLista().values());
							}

							StringBuilder sb = new StringBuilder();

							for (Snake snake : sal.getLista().values()) {
								// if(snake!=null)
								sb.append(getLocationsJson(snake));
								sb.append(',');
							}

							sb.deleteCharAt(sb.length() - 1);

							// l.lock();
							String msg = String.format("{\"type\": \"update\", \"data\" : [%s]}", sb.toString());
							broadcast(msg, sal);
							// l.unlock();
						}
					}
				}
			} catch (Throwable ex) {
				System.err.println("Exception processing tick()");
				ex.printStackTrace(System.err);
			}
		}
	}
	
	//Devuelve un JSON con la posicion de la seprpiente dada
	private String getLocationsJson(Snake snake) {

		synchronized (snake) {

			StringBuilder sb = new StringBuilder();
			sb.append(String.format("{\"x\": %d, \"y\": %d}", snake.getHead().x, snake.getHead().y));
			for (Location location : snake.getTail()) {
				sb.append(",");
				sb.append(String.format("{\"x\": %d, \"y\": %d}", location.x, location.y));
			}

			return String.format("{\"id\":%d,\"body\":[%s]}", snake.getId(), sb.toString());
		}
	}
	
	//Obtiene la sala con el nombre indicado
	public Sala getSala(String nombre) {
		Sala s = null;

		for (ConcurrentHashMap.Entry<Integer, Sala> entry : salas.entrySet()) {
			String key = entry.getValue().getName();
			if (nombre.equals(key)) {
				s = entry.getValue();
				return s;
			}

		}
		return s;
	}

	//Obtiene una sala aleatoria //No se utiliza
	public Sala getRandomSala() {
		int size = salas.size();
		int item = new Random().nextInt(size);
		int i = 0;

		for (ConcurrentHashMap.Entry<Integer, Sala> entry : salas.entrySet()) {
			if (i == item)
				return entry.getValue();
			i++;
		}
		return null;
	}
	
	//Obtiene una sala aleatoria con huecos disponibles, en caso de no haber
	//devuelve null
	public Sala getRandomAvailabeSala() {
		ArrayList<Sala> sals = new ArrayList<>();

		for (Sala s : salas.values()) {

			if (s.contador.availablePermits() != 0) {
				sals.add(s);
			}

			System.out.println("VALOR: " + s.getName());
		}

		int size = sals.size();

		if (size != 0) {
			int item = (int) Math.floor(Math.random() * size);
			System.out.println("VALOR: " + item);

			return sals.get(item);
		}

		return null;
	}
	
	//Manda un mensaje a todas las serpientes de una sala concreta
	public synchronized void broadcast(String message, Sala sala) throws Exception {

		for (Snake snake : sala.getLista().values()) {
			try {
				// System.out.println("Sending message " + message + " to " +
				// snake.getId());

				snake.sendMessage(message);

			} catch (Throwable ex) {
				System.err.println("Execption sending message to snake " + snake.getId());
				ex.printStackTrace(System.err);
				removeSnake(snake);
			}
		}

	}
	
	//Envia un mensaje a todas las serpientes del chat
	public synchronized void broadcastChat(String message) throws Exception {
		chatGlobal.broadcast(message);
	}
	
	//Elimina a una serpiente de la sala de chat si existe
	public boolean removeChatPlayer(String n) {
		return chatGlobal.removeIfExists(n);
	}
	
	//Inicia el excecutor programado hada cierto tiempo
	public void startTimer() {
		scheduler = Executors.newScheduledThreadPool(1);
		scheduler.scheduleAtFixedRate(() -> tick(), TICK_DELAY, TICK_DELAY, TimeUnit.MILLISECONDS);
	}
	
	//Detiene el executor
	public void stopTimer() {
		if (scheduler != null) {
			scheduler.shutdown();
		}
	}
	
	//Genera una nueva posicion para la comido y manda el mensaje
	public void generarComida(Sala sal) throws Exception {

		Location l = SnakeUtils.getRandomLocation();
		Comida com = new Comida(l);
		sal.setComida(com);

		String msg = String.format("{\"type\": \"comida\", \"x\": %d,\"y\":\"%d\"}", l.x, l.y);
		broadcast(msg, sal);

	}
	
	//Obtiene Un string con las mejores serpientes de todas las salas
	public String Mejores() {

		ArrayList<Snake> ordenado = new ArrayList<Snake>(snakesMuro.values());
		// ArrayList <Snake> sol = new ArrayList<Snake>();
		Comparator<Snake> comp = new Comparator<Snake>() {
			@Override
			public int compare(Snake s1, Snake s2) {
				return new Integer(s2.getPuntuacion()).compareTo(new Integer(s1.getPuntuacion()));
			}

		};
		Collections.sort(ordenado, comp);

		String sol = "";

		for (int i = 0; i < ordenado.size(); i++) {

			if (i == 9) {
				break;
			}
			if (i == ordenado.size() - 1) {
				sol += "{\"Nombre\": \"" + ordenado.get(i).getName() + "\", \"Puntuacion\": "
						+ ordenado.get(i).getPuntuacion() + "}";
			} else {
				sol += "{\"Nombre\": \"" + ordenado.get(i).getName() + "\", \"Puntuacion\": "
						+ ordenado.get(i).getPuntuacion() + "},";
			}
		}
		return sol;
	}
	
	//Escribe por consola todas las posiciones de todas las serpientes en el juego
	public void pintar() {
		System.out.println("---------------------Serpientes del juego------------------------------------");
		for (Snake s : snakes.values()) {
			System.out.println(s.getName());
		}

		System.out.println("-------------------Salas del juego ---------------------------------------");
		for (Sala sal : salas.values()) {
			System.out.println(sal.getName());
		}

		System.out.println("-------------------Serpientes de cada sala-----------------------------");
		for (Sala sala : salas.values()) {
			System.out.println("-------------Serpientes de la sala: " + sala.getName() + "------------------");
			for (Snake ss : sala.getLista().values()) {
				System.out.println(ss.getName());
			}
		}
	}
	
	//Resetea valores del juego
	public void resetServer() {

		numSalas.set(0);
		snakeIds.set(0);
		salasIds.set(0);
		numSnakes.set(0);
		salas.clear();
		snakes.clear();
		stopTimer();

	}

	// Getters y setters
	public ConcurrentHashMap<Integer, Snake> getSnakes() {
		return snakes;
	}

	public ConcurrentHashMap<Integer, Snake> getSnakesMuro() {
		return snakesMuro;
	}

	public synchronized int getNumSalas() {
		return this.numSalas.get();
	}

	public void DecSalas() {
		this.numSalas.getAndDecrement();
	}

	public void lock() {
		this.l.lock();
	}

	public void unlock() {
		this.l.unlock();
	}

	public long getTICK_DELAY() {
		return TICK_DELAY;
	}

	public void setTICK_DELAY(int diffculty) {
		TICK_DELAY = ticks[diffculty];
	}
	
	public ConcurrentHashMap<Integer, Snake> getSalaChat() {
		return chatGlobal.getLista();
	}

}
