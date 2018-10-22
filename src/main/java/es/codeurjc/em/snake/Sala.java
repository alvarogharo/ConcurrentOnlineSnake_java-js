package es.codeurjc.em.snake;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Sala {
	private int id; // identificador de la sala
	private ConcurrentHashMap<Integer, Snake> snakes = new ConcurrentHashMap<>();

	Semaphore contador;
	boolean partida_empezada = false;
	private int contadorComida = 0;

	private String nombre;
	private Snake Creador;
	private Comida comida;
	private Thread bloqueado;

	private boolean pulsadoMuro = false;

	Sala(int id, String nombre, Snake creador) {
		this.id = id;
		this.Creador = creador;
		this.nombre = nombre;
		contador = new Semaphore(4, true);
		setComida(null);
	}
	
	//Elimina un jugador de la sala
	void EliminarJugador(Snake jugador) {

		snakes.remove(Integer.valueOf(jugador.getId()));
		contador.release();

	}
	
	//Añade un jugador a la sala si es posible
	boolean AñadirJugador(Snake jugador) {

		if (contador.availablePermits() == 0) {
			String m = "{\"type\": \"espera\"}";
			jugador.sendMessage(m);

		}
		System.out.println("PERMISOS DEL SEMAFORO " + contador.availablePermits());
		try {
			if (contador.tryAcquire(5000, TimeUnit.MILLISECONDS)) {

				snakes.put(jugador.getId(), jugador);
				System.out.println("----------------------LISTA SALA-----------------");
				for (Snake n : snakes.values()) {
					System.out.println(n.getId());

				}
				System.out.println("----------------------FIN-----------------");

				return true;

			}
		} catch (InterruptedException e) {
		}
		return false;
	}
	
	
	//Getters y Setters
	Snake getCreador() {
		return this.Creador;
	}

	int getId() {
		return this.id;
	}

	String getName() {
		return this.nombre;
	}

	ConcurrentHashMap<Integer, Snake> getLista() {
		return this.snakes;
	}

	public Comida getComida() {
		return comida;
	}

	public void setComida(Comida comida) {
		this.comida = comida;
	}

	public int getContadorComida() {
		return contadorComida;
	}

	public void setContadorComida(int contadorComida) {
		this.contadorComida = contadorComida;
	}

	public boolean logSerp() {
		boolean longit = false;
		for (Snake snake : snakes.values()) {
			if (snake.getLenght() > 7) {
				longit = true;
			}
		}
		return longit;
	}

	public boolean muro() {
		return this.pulsadoMuro = true;
	}

	public boolean getPulsadoMuro() {
		return this.pulsadoMuro;
	}

}
