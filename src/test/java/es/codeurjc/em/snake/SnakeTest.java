package es.codeurjc.em.snake;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.*;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.*;

public class SnakeTest {
	AtomicInteger contador = new AtomicInteger(0);


	@BeforeClass
	public static void startServer() {
		Application.main(new String[] { "--server.port=8080" });
	}

	@Test
	public void testConnection() throws Exception {
		System.out.println("---------------------------------------TEST_CONEXION--------------------------------------");
		WebSocketClient wsc = new WebSocketClient();
		wsc.connect("ws://127.0.0.1:8080/snake");
		wsc.disconnect();
	}

	/*Se comprueba que al llegar a 4 jugadores el juego se inicia automaticamente*/
	@Test
	public void testJoin() throws Exception {
		System.out.println("---------------------------------------TEST_JOIN--------------------------------------");

		contador.set(0);
		CyclicBarrier c = new CyclicBarrier(5);
		Executor executor = Executors.newFixedThreadPool(4);

		AtomicReferenceArray<String> firstMsg = new AtomicReferenceArray<String>(4);

		/*codigo que vamos a ejecutar*/
		Runnable tarea = () -> {

			int id = Character.getNumericValue(
					Thread.currentThread().getName().charAt(Thread.currentThread().getName().length() - 1) - 1);

			WebSocketClient wsc = new WebSocketClient();

			wsc.onMessage((session, msg) -> {

				System.out.println("TestMessage: " + msg);
				/*Si el mensaje es el que queremos comprobar lo guardamos en la lista de mensajes junto con el id del hilo*/
				if (msg.contains("update")) {
					firstMsg.compareAndSet(id, null, msg);
				}
			});

			try {
				/*Nos conectamos al servidor*/
				wsc.connect("ws://127.0.0.1:8080/snake");

				System.out.println("Connected");

				String msg;
				System.out.println("Nombre " + Thread.currentThread().getName());
				/*Si es el primer thread crea la sala. Se crea el mensaje*/
				if (contador.getAndIncrement() == 0) {
					msg = String.format(
							"{\"type\": \"user\", \"user\": \"%s\", \"ComandoSala\":\"Crear\",\"Sala\":\"1\", \"difficulty\":\"0\"}",
							"Creador");

				} else {
					/*Si no somos el primer thread nos unimos a la sala*/
					msg = String.format(
							"{\"type\": \"user\", \"user\": \"%s\", \"ComandoSala\":\"Unir\",\"Sala\":\"1\", \"difficulty\":\"0\"}",
							"Union");
				}
				/*enviamos el mensaje al servidor*/
				wsc.sendMessage(msg);
				Thread.sleep(1500);
				/*mandamos un mensaje de finPartida para terminar la ejecucion*/
				String ms = String.format("{\"type\": \"finPartida\"}");
				wsc.sendMessage(ms);
				/*nos desconectamos del servidor*/
				wsc.disconnect();
				c.await();

			} catch (Exception e) {
				e.printStackTrace();
			}

		};

		/*inicializamos las tareas con el executor*/
		executor.execute(tarea);
		Thread.sleep(500);
		executor.execute(tarea);
		executor.execute(tarea);
		executor.execute(tarea);
		Thread.sleep(2500);
		/*Esperamos a que todos los threads terminen la ejecucion*/
		c.await();

		/*Comprobamos que se obtiene update que es el mensaje que indica que se ha inicializado el juego*/
		for (int h = 0; h < 4; h++) {
			String msg = firstMsg.get(h);
			assertTrue("The first message should contain 'update', but it is " + msg, msg.contains("update"));
		}

	}

	/*Cuando hay dos jugadores el creador puede iniciar el juego*/
	@Test
	public void testIniciar() throws Exception {
		System.out.println("---------------------------------------TEST_INICIAR--------------------------------------");
		contador.set(0);
		CyclicBarrier c = new CyclicBarrier(3);
		Executor executor = Executors.newFixedThreadPool(2);

		AtomicReferenceArray<String> firstMsg = new AtomicReferenceArray<String>(3);

		Runnable tarea = () -> {

			int id = Character.getNumericValue(
					Thread.currentThread().getName().charAt(Thread.currentThread().getName().length() - 1) - 1);

			WebSocketClient wsc = new WebSocketClient();

			wsc.onMessage((session, msg) -> {
				System.out.println("TestMessage: " + msg);

				/*Si el mensaje es iniciar lo guardamos en la lista en la tercera posicion.
				Las dos primeras son para los updates (mensaje que se envia cuando el creador puede iniciar la partida*/
				if (msg.contains("iniciar")) {

					firstMsg.compareAndSet(2, null, msg);

					/*Se envia el comando de inicio*/
					try {
						wsc.sendMessage("{\"type\":\"Init\"}");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				/*Si el mensaje es update (comienza el juego) se almacena para comprobar que funciona*/
				if (msg.contains("update")) {

					firstMsg.compareAndSet(id, null, msg);

				}

			});

			/*Nos conectamos y creamos la sala y el segundo jugador*/
			try {
				wsc.connect("ws://127.0.0.1:8080/snake");

				System.out.println("Connected");

				String msg;
				System.out.println("Nombre " + Thread.currentThread().getName());
				if (contador.getAndIncrement() == 0) {
					msg = String.format(
							"{\"type\": \"user\", \"user\": \"%s\", \"ComandoSala\":\"Crear\",\"Sala\":\"1\", \"difficulty\":\"0\"}",
							"Creador");

				} else {

					msg = String.format(
							"{\"type\": \"user\", \"user\": \"%s\", \"ComandoSala\":\"Unir\",\"Sala\":\"1\", \"difficulty\":\"0\"}",
							"Union");
				}
				wsc.sendMessage(msg);

				Thread.sleep(5000);
				/*dejamos el juego y cerramos la sesion*/
				String ms = String.format("{\"type\": \"finPartida\"}");
				wsc.sendMessage(ms);
				wsc.disconnect();
				c.await();
			} catch (Exception e) {
				e.printStackTrace();
			}

		};

		/*Ejecutamos las tareas*/
		executor.execute(tarea);
		Thread.sleep(500);
		executor.execute(tarea);
		Thread.sleep(7000);
		c.await();

		/*Comprobamos que los mensajes son los que queremos los dos primeros de inicio de juego y el tercero el que nos permite 
		iniciar el juego con dos jugadores*/
		String msg = firstMsg.get(0);
		assertTrue("The first message should contain 'update', but it is " + msg, msg.contains("update"));
		msg = firstMsg.get(1);
		assertTrue("The first message should contain 'update', but it is " + msg, msg.contains("update"));
		msg = firstMsg.get(2);
		assertTrue("The first message should contain 'update', but it is " + msg, msg.contains("iniciar"));

	}

	/*Se comprueba que el juego finaliza cuando solo queda un jugador*/
	@Test
	public void testFin() throws Exception {

		System.out.println("---------------------------------------TEST_FIN--------------------------------------");
		contador.set(0);

		CyclicBarrier c = new CyclicBarrier(3);
		Executor executor = Executors.newFixedThreadPool(2);

		AtomicReferenceArray<String> firstMsg = new AtomicReferenceArray<String>(3);


		/*CODIGO DEL CREADOR DE LA SALA*/
		Runnable tareaCreador = () -> {

			int id = Character.getNumericValue(
					Thread.currentThread().getName().charAt(Thread.currentThread().getName().length() - 1) - 1);

			WebSocketClient wsc = new WebSocketClient();

			wsc.onMessage((session, msg) -> {

				System.out.println("TestMessage: " + msg);
				/*Si una serpiente manda leave se cierra el juego y no se puede seguir*/
				if (msg.contains("leave")) {
					firstMsg.compareAndSet(2, null, msg);
				}
				/*Se comprueba que se inicia el juego para las dos serpientes*/
				if (msg.contains("update")) {
					firstMsg.compareAndSet(id, null, msg);
				}

				/*Si podemos iniciar el juego lo iniciamos*/
				if (msg.contains("iniciar")) {

					try {
						wsc.sendMessage("{\"type\":\"Init\"}");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			});

			try {
				/*Nos conectamos al servidor, enviamos el mensaje para crear la sala, esperamos un tiempo y finalizamos la partida*/
				wsc.connect("ws://127.0.0.1:8080/snake");

				System.out.println("Connected");

				String msg;
				System.out.println("Nombre " + Thread.currentThread().getName());

				msg = String.format(
						"{\"type\": \"user\", \"user\": \"%s\", \"ComandoSala\":\"Crear\",\"Sala\":\"1\", \"difficulty\":\"0\"}",
						"Creador");

				wsc.sendMessage(msg);

				Thread.sleep(6500);
				String ms = String.format("{\"type\": \"finPartida\"}");
				wsc.sendMessage(ms);
				wsc.disconnect();
				c.await();
			} catch (Exception e) {
				e.printStackTrace();
			}

		};

		/*CODIGO DEL USUARIO QUE SE UNE A LA SALA*/
		/*Igual que crear sala pero se desconecta antes del servidor*/
		Runnable tareaUnir = () -> {

			int id = Character.getNumericValue(
					Thread.currentThread().getName().charAt(Thread.currentThread().getName().length() - 1) - 1);

			WebSocketClient wsc = new WebSocketClient();

			wsc.onMessage((session, msg) -> {

				System.out.println("TestMessage: " + msg);
				if (msg.contains("update")) {
					firstMsg.compareAndSet(id, null, msg);
				}
			});

			try {
				wsc.connect("ws://127.0.0.1:8080/snake");

				System.out.println("Connected");

				String msg;
				System.out.println("Nombre " + Thread.currentThread().getName());

				msg = String.format(
						"{\"type\": \"user\", \"user\": \"%s\", \"ComandoSala\":\"Unir\",\"Sala\":\"1\", \"difficulty\":\"0\"}",
						"Union");

				wsc.sendMessage(msg);

				Thread.sleep(4000);
				String ms = String.format("{\"type\": \"finPartida\"}");
				wsc.sendMessage(ms);
				wsc.disconnect();
				c.await();
			} catch (Exception e) {
				e.printStackTrace();
			}

		};

		/*Se ejecuta el codigo de los dos threads*/
		executor.execute(tareaCreador);
		Thread.sleep(500);
		executor.execute(tareaUnir);
		Thread.sleep(8000);
		c.await();

		/*Comprobamos que los mensajes son los esperados*/
		String msg = firstMsg.get(0);
		assertTrue("The first message should contain 'update', but it is " + msg, msg.contains("update"));
		msg = firstMsg.get(1);
		assertTrue("The first message should contain 'update', but it is " + msg, msg.contains("update"));
		msg = firstMsg.get(2);
		assertTrue("The first message should contain 'leave', but it is " + msg, msg.contains("leave"));

	}

	/*Se comprueba que si 5 jugadores intentan entrar en una sala uno se queda en espera 5 segundos y si otro jugador
	deja la sala este entrará automaticamente.*/
	@Test
	public void testEspera() throws Exception {
		System.out.println("---------------------------------------TEST_ESPERA--------------------------------------");

		contador.set(0);
		CyclicBarrier c = new CyclicBarrier(6);
		Executor executor = Executors.newFixedThreadPool(5);

		AtomicReferenceArray<String> firstMsg = new AtomicReferenceArray<String>(7);

		Runnable tarea = () -> {

			int id = Character.getNumericValue(Thread.currentThread().getName().charAt(Thread.currentThread().getName().length() - 1) - 1);

			WebSocketClient wsc = new WebSocketClient();

			wsc.onMessage((session, msg) -> {

				System.out.println("TestMessage: " + msg);
				/*Se comprueba que las serpientes se han añadido a la sala*/
				if (msg.contains("join")) {
					firstMsg.compareAndSet(id, null, msg);
				}
				/*Se comprueba que la 5a serpiente se queda en espera*/
				if (msg.contains("espera")) {
					firstMsg.compareAndSet(5, null, msg);
				}
				/*Se comprueba que una de las serpientes deja la partida*/
				if (msg.contains("leave")) {
					firstMsg.compareAndSet(6, null, msg);
				}
			});

			try {
				/*Nos conectamos al servidor, creamos la sala si es el primer thread, esperamos y salimos de la sala*/
				wsc.connect("ws://127.0.0.1:8080/snake");

				System.out.println("Connected");

				String msg;
				System.out.println("Nombre " + Thread.currentThread().getName());
				if (contador.getAndIncrement() == 0) {
					msg = String.format(
							"{\"type\": \"user\", \"user\": \"%s\", \"ComandoSala\":\"Crear\",\"Sala\":\"1\", \"difficulty\":\"0\"}",
							"Creador");

					wsc.sendMessage(msg);
					Thread.sleep(1500);
					wsc.disconnect();
					c.await();
				} else {
					msg = String.format(
							"{\"type\": \"user\", \"user\": \"%s\", \"ComandoSala\":\"Unir\",\"Sala\":\"1\", \"difficulty\":\"0\"}",
							"Union");
					wsc.sendMessage(msg);
					Thread.sleep(3000);
					wsc.disconnect();
					c.await();
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

		};

		/*Ejecutamos las tareas*/
		executor.execute(tarea);
		Thread.sleep(500);
		executor.execute(tarea);
		executor.execute(tarea);
		executor.execute(tarea);

		executor.execute(tarea);

		Thread.sleep(4000);
		c.await();

		/*Comprobamos todos los threads entran en el juego*/
		for (int h = 0; h < 5; h++) {
			String msg = firstMsg.get(h);
			assertTrue("The first message should contain 'join', but it is " + msg, msg.contains("join"));
		}

		/*Comprobamos que el 5 tiene que esperar*/
		String msg = firstMsg.get(5);
		assertTrue("The first message should contain 'espera', but it is " + msg, msg.contains("espera"));

		/*Comprobamos que uno de los threads se va*/
		msg = firstMsg.get(6);
		assertTrue("The first message should contain 'leave', but it is " + msg, msg.contains("leave"));

	}


	/*Se conectan 10 jugadores, se crean salas, se sale de las salas, se crean otras dos salas, se añaden los jugadores,
	tras 10 segundos salen de la sala y se comprueban las puntuaciones del muro*/
	@Test
	public void testCarga() throws InterruptedException, BrokenBarrierException {
		System.out.println("---------------------------------------TEST_CARGA--------------------------------------");

		String nombres[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" };
		contador.set(0);
		CyclicBarrier c = new CyclicBarrier(11);
		Executor executor = Executors.newFixedThreadPool(10);

		/*Array para comprobar los mensajes de Okcrear*/
		AtomicReferenceArray<String> firstMsg = new AtomicReferenceArray<String>(10);

		/*Cada jugador crea una sala*/
		for (String s : nombres) {

			Runnable tarea = () -> {

				int id = Character.getNumericValue(s.charAt(0));

				WebSocketClient wsc = new WebSocketClient();

				wsc.onMessage((session, msg) -> {

					System.out.println("TestMessage: " + msg);
					/*Se comprueba que se ha creado correctamente la sala y se almacena en las 10 primeras posiciones*/
					if (msg.contains("Okcrear")) {
						firstMsg.compareAndSet(id, null, msg);
					}
				});

				try {
					/*Nos conectamos al servidor, creamos una sala, esperamos 2 segundos y salimos*/
					wsc.connect("ws://127.0.0.1:8080/snake");

					System.out.println("Connected");

					String msg;
					System.out.println("Nombre " + Thread.currentThread().getName());

					msg = String.format(
							"{\"type\": \"user\", \"user\": \"%s\", \"ComandoSala\":\"Crear\",\"Sala\":\"%s\", \"difficulty\":\"0\"}",
							"Creador" + s, s);
					System.out.println(msg);

					wsc.sendMessage(msg);

					Thread.sleep(2000);

					wsc.disconnect();
					c.await();

				} catch (Exception e) {
					e.printStackTrace();
				}

			};
			/*Ejecuta la tarea*/
			executor.execute(tarea);

		}


		/*Esperamos 3 segundos*/
		Thread.sleep(3000);
		c.await();

		/*Comprobamos que los mensajes de OkCrear son los correctos*/
		for (int h = 0; h < 10; h++) {
			String msg = firstMsg.get(h);
			System.out.println(msg);
			assertTrue("The first message should contain 'Okcrear', but it is " + msg, msg.contains("Okcrear"));
		}


		/*EL JUGADOR 1 Y 6 CREAN UNA SALA...*/

		/*Arrays para almacenar los mensajes a comprobar*/
		AtomicReferenceArray<String> secondMsg = new AtomicReferenceArray<String>(10);
		AtomicReferenceArray<String> puntuaciones = new AtomicReferenceArray<String>(10);

		Executor executor2 = Executors.newFixedThreadPool(10);
		CyclicBarrier barrera = new CyclicBarrier(11);

		/*Los jugadores 1 y 6 crean una sala*/
		String nombresCreador[] = { "0", "5" };
		for (String s : nombresCreador) {

			Runnable tareaCreador = () -> {
				/*Creamos una sala*/
				int id = Character.getNumericValue(s.charAt(0));
				WebSocketClient wsc = new WebSocketClient();

				wsc.onMessage((session, msg) -> {
					System.out.println("TestMessage: " + msg);
					/*almacenamos el mensaje para comprobar que nos hemos unido a la partida*/
					if (msg.contains("join")) {
						secondMsg.compareAndSet(id, null, msg);
					}
					/*almacenamos el mensaje que nos indica que se muestra el muro*/
					if (msg.contains("muro")) {
						puntuaciones.compareAndSet(id, null, msg);
					}
				});

				try {
					/*Nos conectamos al servidor, creamos la sala, esperamos 10 segundos, salimos y solicitamos el muro*/
					wsc.connect("ws://127.0.0.1:8080/snake");

					System.out.println("Connected");

					String msg;
					System.out.println("Nombre " + Thread.currentThread().getName());
					String sala;

					if (s.equals("0")) {
						sala = "SalaA";
					} else {
						sala = "SalaB";
					}

					msg = String.format(
							"{\"type\": \"user\", \"user\": \"%s\", \"ComandoSala\":\"Crear\",\"Sala\":\"%s\", \"difficulty\":\"0\"}",
							"Creador" + s, sala);
					System.out.println(msg);

					wsc.sendMessage(msg);

					Thread.sleep(10000);

					msg = "{\"type\": \"Muro\"}";
					System.out.println(msg);
					wsc.sendMessage(msg);
					Thread.sleep(1000);

					wsc.disconnect();
					barrera.await();

				} catch (Exception e) {
					e.printStackTrace();
				}

			};
			/*Ejecutamos los creadores*/
			executor2.execute(tareaCreador);

		}
		Thread.sleep(500);

		/*El resto de jugadores su unen a la sala que les corresponde*/
		String nombresUnir[] = { "1", "2", "3", "4", "6", "7", "8", "9" };
		for (String s : nombresUnir) {

			Runnable tareaUnion = () -> {

				int id = Character.getNumericValue(s.charAt(0));

				WebSocketClient wsc = new WebSocketClient();

				wsc.onMessage((session, msg) -> {

					System.out.println("TestMessage: " + msg);
					/*Comprobamos que todos se unen correctamente*/
					if (msg.contains("join")) {
						secondMsg.compareAndSet(id, null, msg);
					}
					/*Comprobamos que el jugador 5 y el 10 se quedan esperando*/
					if (msg.contains("espera")) {
						secondMsg.compareAndSet(id, null, msg);
					}
					/*Comprobamos que todos obtienen el muro*/
					if (msg.contains("muro")) {
						puntuaciones.compareAndSet(id, null, msg);
					}
				});

				try {
					/*Nos conectamos al servidor, nos unimos a la sala que corresponde, esperamos 10 segundos, salimos y 
					solicitamos el muro*/
					wsc.connect("ws://127.0.0.1:8080/snake");

					System.out.println("Connected");

					String msg;
					System.out.println("Nombre " + Thread.currentThread().getName());
					String sala;

					if (s.equals("1") || s.equals("2") || s.equals("3") || s.equals("4")) {
						sala = "SalaA";
					} else {

						sala = "SalaB";
					}

					msg = String.format(
							"{\"type\": \"user\", \"user\": \"%s\", \"ComandoSala\":\"Unir\",\"Sala\":\"%s\", \"difficulty\":\"0\"}",
							"Union" + s, sala);
					System.out.println(msg);

					wsc.sendMessage(msg);

					Thread.sleep(10000);

					msg = "{\"type\": \"Muro\"}";
					System.out.println(msg);
					wsc.sendMessage(msg);
					Thread.sleep(1000);

					wsc.disconnect();
					barrera.await();

				} catch (Exception e) {
					e.printStackTrace();
				}

			};
			/*ejecutamos los hilos que se añaden a las salas ya creadas*/
			executor2.execute(tareaUnion);
		}

		Thread.sleep(14000);
		barrera.await();

		/*comprobamos que todos obtengan join o espera*/
		for (int h = 0; h < 10; h++) {
			String msg = secondMsg.get(h);
			assertTrue("The first message should contain 'join'or'espera', but it is " + msg,
					msg.contains("join") || msg.contains("espera"));
		}

		
		/*Comprobamos que todos han solicitado el muro*/
		String msg = "";
		for (int k= 0;k<10 ;k++ ) {
			msg = puntuaciones.get(k);
			assertTrue("The first message should contain 'muro', but it is " + msg, msg.contains("muro"));
		}

		System.out.println(msg);	
	}
}