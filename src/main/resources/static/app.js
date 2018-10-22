
var user;
var sala;
var comandoSala;
var chat = false;
var difficulty = 0;
var players = [];

/*al principio se pide el nombre del usuario*/
window.onload = function() {

	};

/*obtenemos el nombre del jugador al pulsar aceptar*/	
function actionNombre(){
	
	user = $("#texto1").val()
	
	  console.log(user);

	  /*mostramos los botones de crear sala, unirse a sala y chat*/
	  document.getElementById('btnPrinc').style.display = "block";
	  document.getElementById('nombre').style.display = "none";		  
}

/*Al pulsar crear sala se nos pide introducir nombre de sala y tipo de sala */
function opcCrearSala(){
	/*mostramos los elementos para crear sala y ocultamos lo que no necesitamos*/
	document.getElementById('btnPrinc').style.display = "none";
	document.getElementById('crearSala').style.display = "block";
	document.getElementById('divChat').style.display = "none";
	document.getElementById('console').style.height = "90%";

}
/*Al pulsar crear sala obtenemos todos los valores que necesitamos para crear la sala*/
function actionCrear(){
	/*mostramos el canvas y cultamos el resto de elementos*/
	document.getElementById('canvas').style.display = "block";
	document.getElementById('crearSala').style.display = "none";

	/*obtenemos los valores para crear la sala*/
	sala = $("#texto2").val();
	comandoSala="Crear";
	difficulty = 0;

	if ($('#radio2').prop('checked')){
		difficulty = 1;
	}else if ($('#radio3').prop('checked')){
		difficulty = 2;
	}

	/*Si estamos en el chat eliminamos al usuario del chat*/
	if (!chat){
		juego();
	}else{
		let aux = {"type": "delete", "name": user};
		game.enviar(aux);
		$("#player-box").text("");
		chat = false;
		game.open();
	}	
}

/*Cuando nos unimos a la sala se nos pide el nombre de la sala*/
function opcUnirSala(){
	document.getElementById('btnPrinc').style.display = "none";
	document.getElementById('unirSala').style.display = "block";
	document.getElementById('divChat').style.display = "none";
	document.getElementById('console').style.height = "90%";

}

/*Obtenemos los datos de la sala a la que nos queremos unir*/
function actionUnir(){
	document.getElementById('canvas').style.display = "block";
	document.getElementById('unirSala').style.display = "none";

	sala = $("#unir").val();
	comandoSala="Unir";

	/*Si estamos en el chat eliminamos al usuario del chat*/
	if (!chat){
		juego();
	}else{
		let aux = {"type": "delete", "name": user};
		game.enviar(aux);
		$("#player-box").text("");
		chat = false;
		game.open();
	}
}

/*Nos unimos a una sala aleatoria*/
function actionMatchmaking(){
	document.getElementById('btnPrinc').style.display = "none";
	document.getElementById('canvas').style.display = "block";
	document.getElementById('divChat').style.display = "none";
	document.getElementById('console').style.height = "90%";

	comandoSala="MatchMaking";

	/*Si estamos en el chat eliminamos al usuario del chat*/
	if (!chat){
		juego();
	}else{
		let aux = {"type": "delete", "name": user};
		game.enviar(aux);
		$("#player-box").text("");
		chat = false;
		game.open();
	}
}

/*Seleccionamos el chat*/
function opcChat(){
	document.getElementById('divChat').style.display = "block";
	document.getElementById('chat').style.display = "none";

	document.getElementById('console').style.height = "400px";

	/*Se añade al jugador al chat*/
	sala = "-1";
	comandoSala="Chat";
	players.push(new Player(user));
	updatePlayerBox();
	chat =  true;
	juego();
}

/*Mandamon un mensaje por el chat global*/
function actionMensajeChat(){
	
	let mensaje = user+" : "+$("#mensaje").val();
	$("#mensaje").val("");
	let aux = {"type": "chat", "mensaje": mensaje};
	game.enviar(aux);
}

/*No queremos seguir esperando*/
function actionCancelar(){
	var mensaje={"type": "cancelar"};
	game.enviar(mensaje);
}

/*Queremos iniciar el juego antes de que haya 4 jugadores*/
function actionIniciar(){
	Console.log("Action: Has pulsado iniciar");
	var aux = {"type": "Init"};
	document.getElementById('modal3').style.display = 'none';
	document.getElementById('button5').style.display = "none";
	game.enviar(aux);
	
}

/*Queremos mostrar el muro de la fama*/	
function actionMuro(){
	var aux = {"type":"Muro"};	
	document.getElementById('button6').style.display = 'none';
	document.getElementById('modal4').style.display = 'none';
	game.enviar(aux);
}	

/*actualiza los jugadores del chat*/
function updatePlayerBox(){
	$("#player-box").text("");
	$("#player-box").append("<p> Jugadores en el chat: "+players.length+"</p>");
	for (var k = 0; k < players.length; k++) {
		
			$("#player-box").append("<p>"+players[k].name+" -- </p>");
		
	}
}

	
	
var Console = {};

/*Escribe en la consola del juego*/
Console.log = (function(message) {
	var console = document.getElementById('console');
	var p = document.createElement('p');
	p.style.wordWrap = 'break-word';
	p.innerHTML = message;
	console.appendChild(p);
	while (console.childNodes.length > 25) {
		console.removeChild(console.firstChild);
	}
	console.scrollTop = console.scrollHeight;
});
/*Limpia la consola*/
Console.clean = (function() {
	$("#console").text("");
});



class Snake {

	constructor() {
		this.snakeBody = [];
		this.color = null;
	}

	draw(context) {
		for (var pos of this.snakeBody) {
			context.fillStyle = this.color;
			context.fillRect(pos.x, pos.y,
				game.gridSize, game.gridSize);
		}
	}
}

class Player {

	constructor(name) {
		this.name = name;
	}
}

class Comida{
	constructor(x,y,color){
		this.x=x;
		this.y=y;
		this.color = color;
	}

	draw(context){
		
		context.fillStyle = this.color;
		context.fillRect(this.x, this.y,game.gridSize, game.gridSize);
	}
}

class Game {
	
	enviar(mens){
		var pack = JSON.stringify(mens);
		this.socket.send(pack);
	
	}
	
	constructor(){
		this.socket = null;
		this.fps = 30;
		this.nextFrame = null;
		this.interval = null;
		this.direction = 'none';
		this.gridSize = 10;
		this.comida=new Comida(0,0,'#FF0000');
		
		this.skipTicks = 1000 / this.fps;
		this.nextGameTick = (new Date).getTime();
	}

	/*Inicializa el juego*/
	initialize() {	
	
		this.snakes = [];
		let canvas = document.getElementById('playground');
		if (!canvas.getContext) {
			Console.log('Error: 2d canvas not supported by this browser.');
			return;
		}
		
		this.context = canvas.getContext('2d');
		window.addEventListener('keydown', e => {
			
			/*control del movimiento de las serpientes*/
			var code = e.keyCode;
			if (code > 36 && code < 41) {
				switch (code) {
				case 37:
					if (this.direction != 'east')
						this.setDirection('west');
					break;
				case 38:
					if (this.direction != 'south')
						this.setDirection('north');
					break;
				case 39:
					if (this.direction != 'west')
						this.setDirection('east');
					break;
				case 40:
					if (this.direction != 'north')
						this.setDirection('south');
					break;
				}
			}
		}, false);
		
		this.connect();
	}

	/*Manda el mensaje con la direccion pulsada*/
	setDirection(direction) {
		this.direction = direction;
		var aux = {"type":"direction","direction":direction};
		var mens=JSON.stringify(aux);
		this.socket.send(mens);
	}

	/*Inicializa el juego*/
	startGameLoop() {
		this.nextFrame = () => {
			requestAnimationFrame(() => this.run());
		}
		
		this.nextFrame();		
	}

	/*Para el juego*/
	stopGameLoop() {
		this.nextFrame = null;
		if (this.interval != null) {
			clearInterval(this.interval);
		}
	}

	/*pinta en el canvas*/
	draw() {
		this.context.clearRect(0, 0, 640, 480);
		this.comida.draw(this.context);
		for (var id in this.snakes) {			
			this.snakes[id].draw(this.context);
		}
	}

	/*Añade una nueva Snake*/
	addSnake(id, color, name) {
		this.snakes[id] = new Snake();
		this.snakes[id].color = color;
		this.snakes[id].name = name;
	}
	
	getSnake(id) {
		return this.snakes[id].name;
	}
	
	getSnakesLength() {
		return this.snakes.length;
	}

	updateSnake(id, snakeBody) {
		if (this.snakes[id]) {
			this.snakes[id].snakeBody = snakeBody;
		}
	}

	removeSnake(id) {
		this.snakes[id] = null;
		// Force GC.
		delete this.snakes[id];
	}
	
	añadirComida(x,y,color){
		this.comida=new Comida(x,y,color);
		
		Console.log('SALA: Comida x '+x+" Comida y "+y);
	}

	/*Actualiza el juego*/
	run() {
	
		while ((new Date).getTime() > this.nextGameTick) {
			this.nextGameTick += this.skipTicks;
		}
		this.draw();
		this.comida.draw(this.context);
		if (this.nextFrame != null) {
			this.nextFrame();
		}

	}

	/*manda el primer mensaje de cada cliente al servidor*/
	open() {
		var aux = {"type": "user", "user": user, "ComandoSala":comandoSala,"Sala":sala, "difficulty":difficulty};
		var mens=JSON.stringify(aux);
		this.socket.send(mens);

		if (chat == false){
			this.startGameLoop();
			
			var aux = {"type": "ping"};
			var mens=JSON.stringify(aux);
			setInterval(() => this.socket.send(mens), 5000);
		}
	}

	/*conecta al servidor y define los metodos del socket*/
	connect() {
		
		this.socket = new WebSocket("ws://127.0.0.1:8080/snake");

		/*inicia la conexion*/
		this.socket.onopen = () => {
			
			// Socket open.. start the game loop.
			Console.log('Info: WebSocket connection opened.');
			Console.log('Info: Press an arrow key to begin.');

			// enviamos el usuario al servidor
			this.open();
		}

		/*cierra la conexion*/
		this.socket.onclose = () => {
			let aux = {"type": "delete", "name": user};
			game.enviar(aux);
			Console.log('Info: WebSocket closed.');

			this.stopGameLoop();
		}

		/*define las acciones al recibir los diferentes mensajes*/
		this.socket.onmessage = (message) => {

			var packet = JSON.parse(message.data);
			
			switch (packet.type) {
				/*actualiza las serpientes del juego*/
		      case 'update':
		       for (var i = 0; i < packet.data.length; i++) {
		        this.updateSnake(packet.data[i].id, packet.data[i].body);
		       }
		       break;
		       /*añade un nuevo jugador a la sala*/
		      case 'join':		       
		       Console.log("SALA: "+packet.data[0].nombre+" Ha entrado en la sala");
		       for (var j = 0; j < packet.data.length; j++) {
		        this.addSnake(packet.data[j].id, packet.data[j].color,packet.data[j].nombre);
		       }
		       break;
		       /*elimina un jugador de la sala*/
		      case 'leave':
		       Console.log("SALA"+packet.nombre+' ha dejado la partida');
		       this.removeSnake(packet.id);
		       break;
		       /*indica que la serpiente ha muerto*/
		      case 'dead':
		       Console.log('SALA: Tu serpiente ha muerto!');
		       this.direction = 'none';
		       break;
		       /*indica que has cogido la comida*/
		      case 'kill':
		       Console.log('SALA: Comida conseguida!');
		       break;
		       /*indica si la sala ha sido creada con exito o si ya existe*/
		      case 'Okcrear': 
		       if(packet.data==='Ok'){
		        Console.log('Info: Sala '+sala+" creada con éxito"); 
		        document.getElementById('crearSala').style.display = "none";	        
		       }else{
		        Console.log('Alerta: La sala '+sala+" ya existe"); 
		       }
		       break;
		       /*indica si se ha podido añadir a la sala o si no se ha podido añadir*/
		      case 'Okunir':     	  
				   document.getElementById('cancel').style.display = "none";
         		if(packet.data==='Ok'){
          
         		document.getElementById('unirSala').style.display = "none";
          
         		}else if (packet.data==='NotOk'){
					  Console.log("Alerta: Espera cancelada, tiempo agotado"); 
					  $("#unir").val("");
					 document.getElementById('cancel').style.display = "none";
					 document.getElementById('canvas').style.display = "none";
					 document.getElementById('btnPrinc').style.display = "block";
         		}else if (packet.data==='NoSalas') {
					Console.log("Alerta: No hay salas disponibles");
				   document.getElementById('cancel').style.display = "none";
				   document.getElementById('canvas').style.display = "none";
				   document.getElementById('btnPrinc').style.display = "block";
				 }else{
					Console.log("Alerta: La sala no existe"); 
					$("#unir").val("");
				   document.getElementById('cancel').style.display = "none";
				   document.getElementById('canvas').style.display = "none";
				   document.getElementById('btnPrinc').style.display = "block";
				 }
				 break;
			/*escribe el mensaje recibido*/
			case "chat" :
         		Console.log(packet.mensaje);
				 break;
			/*añade un jugador a los jugadores en el chat*/	 
			case "player" :
				 players.push(new Player(packet.name));
				 updatePlayerBox();
				 break;
			/*actualiza todos los jugadores que deben estar en el chat*/	 
			case "players" :
				 var aux = [];
				 var aux2 = [];
				 if (packet.list != ""){
					 aux = packet.list.split(",");

					 for (i = 0;i<aux.length-1 ; i++){
						aux2.push(new Player(aux[i]));
					 }

					 players = aux2;
				 }else{
					 players = [];
				 }
				 updatePlayerBox();
         		break;
        		/*Se cancela la espera para entrar en una sala*/
        	  case "cancelar" :
         		Console.log(packet.info);
         		document.getElementById('cancel').style.display = "none";
         		break;
		       /*Se indica que la sala esta llena y muestra el boton para cancelar la espera*/
		      case "espera":
		    	  Console.log("Alerta: Sala llena esperando 5 segundos");
		    	  document.getElementById('cancel').style.display = "block";
		    	  break;
		    	 /*Indica al creador de la sala que ya hay dos jugadores y le da la posibilidad de iniciar el juego*/
		       case 'iniciar':
		       		Console.log("SALA: Ya puedes iniciar");
		       		document.getElementById('modal3').style.display = "block";
		       		break;
		       	/*Añade comida al juego*/
		       case 'comida':
		    	   this.añadirComida(packet.x,packet.y,'#FF0000');
		    	   break;
		    	/*fin de partida*/
		    	case 'fin':
		    		Console.log("SALA: Fin de partida");
		    		document.getElementById('modal4').style.display = "block";
		    		break;

		    	case 'empezar':
		       		document.getElementById('modal3').style.display = "none";
		       		break;

		       	case 'partidasEnJuego':
		       		Console.log("Alert: Hay partidas en juego. Espere por favor");
		       		document.getElementById('modal4').style.display = "none";
		    		document.getElementById('button6').style.display = "none";

		    	/*Muestra el muro de la fama*/
				case 'muro':
					Console.clean();
					Console.log("------------------------------");
					Console.log("MURO DE LA FAMA");
					Console.log("------------------------------");
		    		for (var m = 0; m < packet.data.length; m++) {
		        		Console.log("Nombre: "+packet.data[m].Nombre+"\nPuntuacion: "+packet.data[m].Puntuacion);
		       		}
		       		var aux = {"type":"finPartida"};
		       		game.enviar(aux);


		    	   
		   }
		  }
		}
	}

let game=new Game();


function juego(){
game.initialize();

}

