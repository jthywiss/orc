package orc;

import java.util.concurrent.BlockingQueue;

import orc.runtime.Environment;
import orc.runtime.OrcEngine;
import orc.runtime.nodes.Node;
import orc.runtime.values.Value;

public class OrcInstance implements Runnable {
	
	OrcEngine engine;
	Node root;
	Environment env;
	BlockingQueue<Value> q;
	boolean running = false;
	
	public OrcInstance(OrcEngine engine, Node root, Environment env, BlockingQueue<Value> q) {
		this.engine = engine;
		this.root = root;
		this.env = env;
		this.q = q;
	}

	public void run() {
		running = true;
		engine.run(root, env);
		running = false;
	}
	
	public BlockingQueue<Value> pubs() { 
		return q; 
	}
	
	public boolean isRunning() {
		return running;
	}

}
