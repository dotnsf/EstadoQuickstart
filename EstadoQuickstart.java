import java.util.Calendar;
import java.util.TimeZone;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;

public class EstadoQuickstart implements MqttCallback {
	MqttClient myClient;
	MqttConnectOptions connOpt;
	
	static final String BROKER_URL = "tcp://quickstart.messaging.internetofthings.ibmcloud.com:1883";	
	static final String dc = "ng";
	
	static String deviceId = "net.bluemix." + dc + ".estado.mqtt.publish.";
	static int interval = 60;

	@Override
	public void connectionLost(Throwable t) {
		// code to reconnect to the broker would go here if desired
	}
	
	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		// TODO Auto-generated method stub
		System.out.println("-------------------------------------------------");
		System.out.println("| Topic:" + topic);
		System.out.println("| Message: " + new String(message.getPayload()));
		System.out.println("-------------------------------------------------");
	}

	public static void main(String[] args) {
		try{
			if( args.length > 0 ){
				interval = Integer.parseInt( args[0] );
			}
			
			String hex = "0123456789abcdef";
			for( int i = 0; i < 8; i ++ ){
				char c = hex.charAt( ( int )( Math.floor( Math.random() * 16 ) ) );
				deviceId += c;
			}
			
		}catch( Exception e ){
		}
		
		EstadoQuickstart eq = new EstadoQuickstart();
		eq.runClient();
	}


	public void runClient() {
		// TODO Auto-generated method stub
		String clientID = "d:quickstart:MyDevice:" + deviceId;
		System.out.println( "deviceId=" + deviceId );
		connOpt = new MqttConnectOptions();
		
		connOpt.setCleanSession( true );
		connOpt.setKeepAliveInterval( 30 );

		// Connect to Broker
		try{
			myClient = new MqttClient( BROKER_URL, clientID );
			myClient.setCallback( this );
			myClient.connect( connOpt );
		}catch( MqttException e ){
			e.printStackTrace();
			System.exit( -1 );
		}

		String myTopic = "iot-2/evt/estado_" + dc + "/fmt/json";
		MqttTopic topic = myClient.getTopic( myTopic );
		
		while( true ){
			try{
				Calendar c = Calendar.getInstance();
				TimeZone tz = TimeZone.getTimeZone( "Asia/Tokyo" );
				c.setTimeZone( tz );;
				int y = c.get( Calendar.YEAR );
				int m = c.get( Calendar.MONTH ) + 1;
				int d = c.get( Calendar.DAY_OF_MONTH );
				int h = c.get( Calendar.HOUR_OF_DAY );
				int n = c.get( Calendar.MINUTE );
				int s = c.get( Calendar.SECOND );
				String dt = y
						+ "/" + ( m < 10 ? "0" : "" ) + m
						+ "/" + ( d < 10 ? "0" : "" ) + d
						+ "T" + ( h < 10 ? "0" : "" ) + h
						+ ":" + ( n < 10 ? "0" : "" ) + n
						+ ":" + ( s < 10 ? "0" : "" ) + s
						+ "+09:00";

				String out = "";
				HttpClient client = new HttpClient();
				GetMethod get = new GetMethod( "http://estado." + dc + ".bluemix.net/" );

				int sc = client.executeMethod( get );
				String html = get.getResponseBodyAsString();
				int x = html.indexOf( "<table" );
				while( x > -1 ){
//				if( x > -1 ){
					int td1 = html.indexOf( "<td>", x + 1 );
					int td2 = html.indexOf( "</td>", td1 + 4 );
					int td3 = html.indexOf( "<td>", td2 + 1 );
					int td4 = html.indexOf( "</td>", td3 + 4 );
					if( td1 > -1 && td2 > -1 && td3 > -1 && td4 > -1 ){
						String name = html.substring( td1 + 4, td2 );
						String svalue = html.substring( td3 + 4, td4 );

						if( svalue.equals( "down" ) ){
							String line = "\"" + name + "\"";
							if( out.length() > 0 ){
								line = "," + line;
							}
							out += line;
						}
						x = td4;
					}
					
					x = html.indexOf( "<tr ", x + 1 );
				}

				out = "{\"datetime\":\"" + dt + "\",\"error_services\":[" + out + "]}";
				//System.out.println( "out = " + out );
				
				//. MQTT Publish
		   		int pubQoS = 0;
				MqttMessage message = new MqttMessage( out.getBytes() );
		    	message.setQos( pubQoS );
		    	message.setRetained( false );

		    	// Publish the message
		    	//System.out.println( "Publishing to topic \"" + topic + "\" qos " + pubQoS );
		    	MqttDeliveryToken token = null;
		    	try{
		    		// publish message to broker
					token = topic.publish( message );
			    	// Wait until the message has been delivered to the broker
					token.waitForCompletion();
					Thread.sleep( 1000 );
				}catch( Exception e ){
					e.printStackTrace();
				}

				//. 次の実行タイミングを待つ
				Calendar c0 = Calendar.getInstance();
				int s0 = ( c0.get( Calendar.SECOND ) % interval );
				int w = 1000 * ( interval - s0 );
				Thread.sleep( w );
			}catch( Exception e ){
			}
		}
	}

}

