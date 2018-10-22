package es.codeurjc.em.snake;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.json.JSONObject;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SnakeHandler extends TextWebSocketHandler {

	private static final String SNAKE_ATT = "snake";

	private SnakeGame snakeGame = new SnakeGame();
	
	private Lock l = new ReentrantLock();
	private Lock lmuro = new ReentrantLock();
	
	Executor executor = Executors.newFixedThreadPool(20);

	String user_name;

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {

	}
	
	//Maneja todos los mensajes recibidos de los clientes
	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		try {
			int id = snakeGame.snakeIds.getAndIncrement();
			String payload = message.getPayload();

			JSONObject json = new JSONObject(payload);

			String tipo = json.getString("type");

			switch (tipo) {

			case "user": //Primer mensaje de cada cliente con su primera accion

				System.out.println("User invocado");

				Runnable tarea = () -> {
					try {
						l.lock();
						String msg; // Tendrá el mensaje de confirmación o no de
									// que la sala existe o no
						String nombre = json.getString("user");
						int difficulty = json.getInt("difficulty");
						snakeGame.setTICK_DELAY(difficulty);
						System.out.println(nombre);
						Snake s = new Snake(id, session, nombre);
						s.setHiloBlock(Thread.currentThread());
						System.out.println("Nombre de usuario " + nombre);
						Sala sal = null;
						session.getAttributes().put(SNAKE_ATT, s);
						
						if (json.getString("ComandoSala").equals("Crear")) { //Crear partida

							// Si no existe la sala la crea
							if (!snakeGame.comprobarSala(json.getString("Sala"))) {

								if (snakeGame.getNumSalas() == 0) {
									snakeGame.startTimer();
								}
								int idSala = snakeGame.salasIds.getAndIncrement();
								String nom = json.getString("Sala");
								System.out.println(nom);

								sal = new Sala(id, nom, s);
								sal.AñadirJugador(s);

								s.setSala(sal);
								System.out.println("Nombre de sala " + nom);

								snakeGame.addSala(sal);
								snakeGame.addSnake(s);

								// snakeGame.lock();
								msg = "{\"type\": \"Okcrear\",\"data\":\"Ok\"}";
								s.sendMessage(msg);
								// snakeGame.unlock();

							} else {

								// snakeGame.lock();
								msg = "{\"type\": \"Okcrear\",\"data\":\"NotOk\"}";
								s.sendMessage(msg);
								// snakeGame.unlock();
								l.unlock();
								return;
							}
							// si el comando es unir:
						} else if (json.getString("ComandoSala").equals("Unir")) { //Unirse a sala

							// si existe la sala (Se tiene que devolver la sala
							// de la lista de salas)
							if (snakeGame.comprobarSala(json.getString("Sala"))) {

								sal = snakeGame.getSala(json.getString("Sala"));

								boolean comprobar = sal.AñadirJugador(s);
								snakeGame.addSnake(s);

								int aux3 = sal.contador.availablePermits();
								if (aux3 == 0) {
									// snakeGame.lock();
									msg = "{\"type\": \"empezar\"}";
									sal.getCreador().sendMessage(msg);
									// snakeGame.unlock();
									sal.partida_empezada = true;

								}
								if (aux3 >= 2 && (!sal.partida_empezada)) {
									// snakeGame.lock();
									msg = "{\"type\": \"iniciar\"}";
									System.out.println("----------------->" + msg);
									sal.getCreador().sendMessage(msg);
									// snakeGame.unlock();
								}

								if (comprobar) { // true si se ha añadido el
													// jugador
									s.setSala(sal);
									// snakeGame.lock();
									msg = "{\"type\": \"Okunir\",\"data\":\"Ok\"}";
									s.sendMessage(msg);
									// snakeGame.unlock();

								} else {
									// snakeGame.lock();
									msg = "{\"type\": \"Okunir\",\"data\":\"NotOk\"}";
									s.sendMessage(msg);
									// snakeGame.unlock();
									l.unlock();
									return;
								}
							} // Sala no existe
							else {
								// snakeGame.lock();
								msg = "{\"type\": \"Okunir\",\"data\":\"NoExiste\"}";
								s.sendMessage(msg);
								// snakeGame.unlock();
								l.unlock();
								return;
							}
						} else if (json.getString("ComandoSala").equals("MatchMaking")) { //Matchmacking

							if (snakeGame.getNumSalas() != 0) {

								sal = snakeGame.getRandomAvailabeSala();

								if (sal == null) {
									msg = "{\"type\": \"Okunir\",\"data\":\"NoSalas\"}";
									s.sendMessage(msg);
									// snakeGame.unlock();
									l.unlock();
									return;
								} else {

									boolean comprobar = sal.AñadirJugador(s);
									snakeGame.addSnake(s);

									int aux3 = sal.contador.availablePermits();
									if (aux3 == 0) {
										// snakeGame.lock();
										msg = "{\"type\": \"empezar\"}";
										sal.getCreador().sendMessage(msg);
										// snakeGame.unlock();
										sal.partida_empezada = true;

									}
									if (aux3 >= 2 && (!sal.partida_empezada)) {
										// snakeGame.lock();
										msg = "{\"type\": \"iniciar\"}";
										System.out.println("----------------->" + msg);
										sal.getCreador().sendMessage(msg);
										// snakeGame.unlock();
									}

									if (comprobar) { // true si se ha añadido el
														// jugador 
										s.setSala(sal);
										// snakeGame.lock();
										msg = "{\"type\": \"Okunir\",\"data\":\"Ok\"}";
										s.sendMessage(msg);
										// snakeGame.unlock();

									} else {
										// snakeGame.lock();
										msg = "{\"type\": \"Okunir\",\"data\":\"NotOk\"}";
										s.sendMessage(msg);
										// snakeGame.unlock();
										l.unlock();
										return;
									}
								}
							} else {
								// snakeGame.lock();
								msg = "{\"type\": \"Okunir\",\"data\":\"NoSalas\"}";
								s.sendMessage(msg);
								// snakeGame.unlock();
								l.unlock();
								return;
							}

						} else if (json.getString("ComandoSala").equals("Chat")) { //Acceso al chat global

							ConcurrentHashMap<Integer, Snake> aux = snakeGame.getSalaChat();
							String mspl = "";
							for (Snake sn : aux.values()) {
								mspl += sn.getName() + ",";
							}
							System.out.println(mspl);
							String msAux = "{\"type\": \"players\",\"list\": \"" + mspl + "\"}";
							s.sendMessage(msAux);
							snakeGame.addSnakeChat(s);

							String msch = "{\"type\": \"player\",\"name\": \"" + s.getName() + "\"}";
							snakeGame.broadcastChat(msch);
							l.unlock();
						}

						if (sal != null) {

							StringBuilder sb = new StringBuilder();
							for (Snake snake : sal.getLista().values()) {
								sb.append(String.format("{\"id\": %d, \"color\": \"%s\",\"nombre\":\"%s\"}",
										snake.getId(), snake.getHexColor(), nombre));
								sb.append(',');
							}
							sb.deleteCharAt(sb.length() - 1);
							String msg2 = String.format("{\"type\": \"join\",\"data\":[%s]}", sb.toString());
							// snakeGame.lock();
							snakeGame.broadcast(msg2, s.getSala());
							// snakeGame.unlock();
							snakeGame.pintar();
							l.unlock();
							// snakeGame.pintar();
						}

					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				};
				executor.execute(tarea);
				break;

			case "direction": //Nueva direccion de una serpiente
				Snake sn = (Snake) session.getAttributes().get(SNAKE_ATT);
				String aux = json.getString("direction");
				System.out.println("------------------------------------------>" + aux);
				Direction d = Direction.valueOf(aux.toUpperCase());
				sn.setDirection(d);
				return;

			case "ping": //Inicio de la conexion
				return;

			case "cancelar": //Cancela la espera si una sala esta llena
				System.out.println("-------------------------------\n---------------------\n cancelar");
				Snake sss = (Snake) session.getAttributes().get(SNAKE_ATT);
				Thread añadir = sss.getHiloBlock();
				añadir.interrupt();
				String ms = "{\"type\": \"cancelar\",\"info\": \"Espera cancelada\"}";
				// snakeGame.lock();
				sss.sendMessage(ms);
				// snakeGame.unlock();
				break;

			case "chat": //Manda los mensajes a todas las serpientes en el chat
				String msch = "{\"type\": \"chat\",\"mensaje\": \"" + json.getString("mensaje") + "\"}";
				snakeGame.broadcastChat(msch);
				break;

			case "delete": //Elimina a una serpiente de la sala de chat
				System.out.println(snakeGame.removeChatPlayer(json.getString("name")));
				System.out.println(json.getString("name"));

				ConcurrentHashMap<Integer, Snake> auxSn = snakeGame.getSalaChat();
				String mspl = "";
				for (Snake snk : auxSn.values()) {
					mspl += snk.getName() + ",";
				}
				System.out.println(mspl);
				String msAux = "{\"type\": \"players\",\"list\": \"" + mspl + "\"}";
				snakeGame.broadcastChat(msAux);
				break;

			case "Init": //Inicializa la partida
				System.out.println("recibido Init");
				Snake s = (Snake) session.getAttributes().get(SNAKE_ATT);
				s.getSala().partida_empezada = true;
				// snakeGame.startTimer();
				break;

			case "Muro": //Manda la informacion del muro a los clientes que la solicitan

				Snake snake = (Snake) session.getAttributes().get(SNAKE_ATT);

				String cadena = snakeGame.Mejores();
				
				String msg2 = String.format("{\"type\": \"muro\",\"data\":[%s]}", cadena);
				System.out.println("-"+msg2+"-");
				snake.sendMessage(msg2);

				break;

			case "finPartida": //Finaliza la partida

			}

			System.out.println(payload);

		} catch (Exception e) {
			System.err.println("Exception processing message " + message.getPayload());
			e.printStackTrace(System.err);
		}
	}
	
	//Cuando se cierran las conexiones elimna la serpiente de las salas en las que estuviera
	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {

		System.out.println("Connection closed. Session " + session.getId());

		Snake s = (Snake) session.getAttributes().get(SNAKE_ATT);

		if (s != null) {
			String msg = String.format("{\"type\": \"leave\", \"id\": %d,\"nombre\":\"%s\"}", s.getId(), s.getName());
			System.out.println("-------------------------------->" + s.getId());

			System.out.println(snakeGame.removeChatPlayer(s.getName()));
			ConcurrentHashMap<Integer, Snake> auxSn = snakeGame.getSalaChat();
			String mspl = "";
			for (Snake snk : auxSn.values()) {
				mspl += snk.getName() + ",";
			}
			System.out.println(mspl);
			String msAux = "{\"type\": \"players\",\"list\": \"" + mspl + "\"}";
			snakeGame.broadcastChat(msAux);

			// snakeGame.lock();
			try {
				snakeGame.broadcast(msg, s.getSala());
				snakeGame.removeSnake(s);
				snakeGame.pintar();
				
			} catch (Exception e) {

			}
			// snakeGame.unlock();

			// snakeGame.pintar();

		}

	}

}
