package net.waefers.peer2peer;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;


public class XorMappingTest {

	static Integer MAX_ID = 99;
	
	static Integer TTL = 256;
	
	static HashMap<Integer,HashSet<Integer>[]> map = new HashMap<Integer,HashSet<Integer>[]>();
	
	static Integer findBest(Integer start,Integer stop)
	{
		Object[] pos = map.get(start)[0].toArray();
		//System.out.print(start+"->"+stop+" possibilities:");
		//for(int x=0;x<pos.length;x++) System.out.print(pos[x]+",");
		//System.out.println();
		Integer best = MAX_ID;
		for(int x=0;x<pos.length;x++)
		{
			Integer now = (Integer)pos[x];
			if(map.get((Integer)start)[1].contains(now))
			{
				//System.out.println("skip "+start+","+now);
				continue;
			}
			if((now^stop) < (best^stop))
				best=now;
		}
		if(best==MAX_ID)
		{
			//System.out.println("Looping! Random route");
			best = (Integer) pos[(int)(Math.random()*pos.length)];
		}
		//System.out.println(start+","+stop+","+best);
		map.get((Integer)start)[1].add(best);
		return best;
	}
	
	@SuppressWarnings("unchecked")
	static void buildMap()
	{
		map.clear();
		for(int x=0;x<32;x++)
		{
			HashSet<Integer>[] a = new HashSet[2];
			a[0] = new HashSet<Integer>();
			a[1] = new HashSet<Integer>();
			map.put((Integer)x,a);
			//System.out.print(x+" ");
			while(map.get((Integer)x)[0].size()<4)
			{
				Integer succ = (Integer)(int)(Math.random()*32);
				if(!map.get((Integer)x)[0].contains(succ))
				{
					map.get((Integer)x)[0].add(succ);
					//System.out.print(succ+",");
				}
			}
			//System.out.println();
		}
	}
	
	static void printMap()
	{
		for(int x=0;x<31;x++)
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
	
	public static void main(String[] args) throws InterruptedException {
		Integer low = 0;
		Integer high = (int)Math.pow(2, Double.parseDouble(args[0]));
		long trials = Long.parseLong(args[1]);
		long benchmark = Long.parseLong(args[2]);
		long total = 0;
		long lost = 0;
		long start = new Date().getTime();
		
		for(int x=1;x<=trials;x++)
		{	
			buildMap();
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
			//printMap();
			total += steps;
			
			if(x%benchmark==0)
				System.out.println("Iterations done: "+x+":::"+(double)x/(double)((new Date().getTime()-start)/1000)+" iterations/sec");
			
		}
		
		System.out.println("Trials: "+trials);
		System.out.println("Average steps: "+(total/trials));
		System.out.println("Lost packets: "+lost);
	}

}
