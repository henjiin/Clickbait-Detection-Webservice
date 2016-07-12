package clickbaitModel;

import java.util.Observable;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
@WebListener
public class StartStop extends Observable implements ServletContextListener  {
	static StartStop instance;
	boolean go =false;
	public StartStop() {
		instance=this;
	}
	static StartStop getInstance(){
		if(instance==null){
			instance = new StartStop();
		}
		return instance;
		
	}
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        System.out.println("Servlet has been started.");
        boolean go = true;
        notifyObservers();
        
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        System.out.println("Servlet has been stopped.");
        notifyObservers();
    }
    

}