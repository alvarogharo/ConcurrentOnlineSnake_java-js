package es.codeurjc.em.snake;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class SalaChat {
	private ConcurrentHashMap<Integer, Snake> snakes = new ConcurrentHashMap<>();

	AtomicInteger contador;

	Thread bloqueado;

	SalaChat() {
		contador = new AtomicInteger(0);
	}
	
	//Elimina un jugador de la sala
	void EliminarJugador(Snake jugador) {

		snakes.remove(Integer.valueOf(jugador.getId()));
		contador.getAndDecrement();

	}
	
	//Añade un jugador a la sala
	void AñadirJugador(Snake jugador){

		snakes.put(jugador.getId(), jugador);
		contador.getAndIncrement();
		System.out.println("----------------------LISTA SALA-----------------");

		for (Snake n : snakes.values()) {
			System.out.println(n.getId());
		}
		System.out.println("----------------------FIN-----------------");
	}
	
	//Envia un mensaje a todas las serpientes de la sala de chat
	public synchronized void broadcast(String message) throws Exception {

		for (Snake snake : snakes.values()) {
			try {
				snake.sendMessage(message);

			} catch (Throwable ex) {
				System.err.println("Execption sending message to snake " + snake.getId());
				ex.printStackTrace(System.err);
			}
		}

	}
	
	//Elimina una serpiente de la sala de char si existe
	public boolean removeIfExists(String n){
		for (Snake s : snakes.values()){
			if (s.getName().equals(n)){
				snakes.remove(Integer.valueOf(s.getId()));
				return true;
			}
		}
		return false;
	}

	//Getters y Setters
		public boolean logSerp() {
			boolean longit = false;
			for (Snake snake : snakes.values()) {
				if (snake.getLenght() > 7) {
					longit = true;
				}
			}
			return longit;
		}

		public AtomicInteger getContador() {
			return contador;
		}

		public void setContador(int contador) {
			this.contador.getAndSet(contador);
		}
		
		public ConcurrentHashMap<Integer, Snake> getLista() {
			return this.snakes;
		}
}
