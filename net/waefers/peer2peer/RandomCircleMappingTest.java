package net.waefers.peer2peer;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;


public class RandomCircleMappingTest {

	static Integer MAX_ID,FAIL_ID;
	
	static Integer TTL = (int) Math.pow(2,20);
	
	static HashMap<Integer,HashSet<Integer>[]> map = new HashMap<Integer,HashSet<Integer>[]>();
	
	static boolean debug = true;
	
	static Integer findBest(Integer start,Integer stop)
	{
		Object[] pos = map.get(start)[0].toArray();
		//System.out.print(start+"->"+stop+" possibilities:");
		//for(int x=0;x<pos.length;x++) System.out.print(pos[x]+",");
		//System.out.println();
		Integer best = FAIL_ID;
		for(int x=0;x<pos.length;x++)
		{
			Integer now = (Integer)pos[x];
			if(map.get((Integer)start)[1].contains(now))
			{
				//System.out.println("skip "+start+","+now);
				continue;
			}
			if(Math.abs(now-stop) < Math.abs(best-stop))
				best=now;
		}
		if(best==FAIL_ID)
		{
			//System.out.println("Looping! Random route");
			best = (Integer) pos[(int)(Math.random()*pos.length)];
		}
		//System.out.println(start+","+stop+","+best);
		map.get((Integer)start)[1].add(best);
		return best;
	}
	
	@SuppressWarnings("unchecked")
	static void buildMap(Integer low, Integer high)
	{
		map.clear();
		for(int x=low;x<high;x++)
		{
			HashSet<Integer>[] a = new HashSet[2];
			a[0] = new HashSet<Integer>();
			a[1] = new HashSet<Integer>();
			map.put((Integer)x,a);
		}
		for(int x=low;x<high;x++)
		{
			print(x+" ");
			long pool = (long) (50*Math.log10(MAX_ID));
			TreeMap<Integer,Integer> distances = new TreeMap<Integer,Integer>();
			for(int y=0;y<pool;y++)
			{
				Integer node =0;
				do
					node = (Integer)(int)(Math.random()*high);
				while(node==x);
				distances.put(Math.abs(x-node),node);
				print(node+":");
			}
			String selectednodes = "";
			Iterator i = distances.keySet().iterator();
			println("");
			print(x+" ");
			while(i.hasNext())
			{
				Object dst = i.next();
				print(distances.get(dst)+"("+dst+")"+"*");
			}
			Integer select=(int)pool/25;
			while(map.get((Integer)x)[0].size()<select)
			{
				//println("Select: "+select+" Size: "+map.get((Integer)x)[0].size());
				Integer target = (int)(Math.pow(8,map.get((Integer)x)[0].size())%(MAX_ID-1));
				Integer node = distances.remove(distances.firstKey());
				Integer next = distances.get(distances.firstKey());
				if( 
					Math.abs((Math.abs(node-x))-target) < Math.abs((Math.abs(next-x))-target) 
					&& !map.get((Integer)x)[0].contains(node)
					&& map.get((Integer)node)[0].size()<select
				)
				{
					map.get((Integer)x)[0].add(node);
					map.get((Integer)node)[0].add(x);
					selectednodes=selectednodes+node+"("+target+")"+",";
				}
			}
			println("");
			println(x+" "+selectednodes);
		}
	}
	
	static void printMap(Integer low, Integer high)
	{
		for(int x=low;x<high;x++)
		{
			Object[] succ = map.get((Integer)x)[0].toArray();
			System.out.print(x+" ");
			for(int y=0;y<succ.length;y++)
			{
				System.out.print(succ[y]+",");
			}
			System.out.println();
		}
	}
	
	static void print(String str)
	{
		if(debug)
			System.out.print(str);
	}
	
	static void println(String str)
	{
		if(debug)
			System.out.println(str);
	}
	
	public static void main(String[] args) throws InterruptedException {
		Integer low = 0;
		Integer high = (int)Math.pow(2, Double.parseDouble(args[0]));
		MAX_ID = high;
		FAIL_ID = MAX_ID+1;
		long trials = Long.parseLong(args[1]);
		long benchmark = Long.parseLong(args[2]);
		long total = 0;
		long lost = 0;
		long start = new Date().getTime();
		
		for(int x=1;x<=trials;x++)
		{	
			buildMap(low,high);
			Integer cur = (int)(Math.random()*(high-low)+low);
			Integer end = (int)(Math.random()*(high-low)+low);
			int steps = 0;
			while (cur != end && steps != TTL)
			{
				cur = findBest(cur,end);
				steps++;
				//Thread.sleep(200);
			}
			if(steps==TTL)
				lost++;
			//System.out.println("Done");
			//System.out.println(steps+" steps");
			printMap(low,high);
			total += steps;
			
			if(x%benchmark==0)
				System.out.println("Iterations done: "+x+":::"+(double)x/(double)((new Date().getTime()-start)/1000)+" iterations/sec");
			
		}
		System.out.println("Hash size: "+high+" ("+args[0]+" bits)");
		System.out.println("Trials: "+trials);
		System.out.println("Average steps: "+(total/trials));
		System.out.println("Lost packets: "+lost);
	}
}
