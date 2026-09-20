package clipboard;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeSet;
public class Foo {
	private static void extendOrStart(Map<Integer,TreeSet<Integer>> increasingMap,Map<Integer,TreeSet<Integer>> decreasingMap,int x) {
		TreeSet<Integer> seq=decreasingMap.get(x+1);
		TreeSet<Integer> other=increasingMap.get(x-1);
		if(seq!=null) {
			increasingMap.remove(seq.last());
			decreasingMap.remove(seq.first());
		}
		if(other!=null) {
			increasingMap.remove(other.last());
			decreasingMap.remove(other.first());
		}
		if(seq==null) seq=other;
		else if(other!=null) seq.addAll(other);
		if(seq==null) seq=new TreeSet<>();
		seq.add(x);
		increasingMap.put(seq.last(),seq);
		decreasingMap.put(seq.first(),seq);
	}
	public static void main(String[] args) {
		List<Integer> numbers=new ArrayList<>(List.of(4,5,6,2,3,4,9,8,1,7));
		System.out.println("Input: "+numbers);
		Map<Integer,TreeSet<Integer>> increasingMap=new HashMap<>();
		Map<Integer,TreeSet<Integer>> decreasingMap=new HashMap<>();
		TreeSet<Integer> seen=new TreeSet<>();
		for(int x:numbers) {
			if(seen.contains(x)) {
				System.out.println("Skipping duplicate: "+x);
				continue;
			}
			seen.add(x);
			extendOrStart(increasingMap,decreasingMap,x);
			// System.out.println("After "+x+":");
			// System.out.println(" increasing: "+increasingMap);
			// System.out.println(" decreasing: "+decreasingMap);
		}
		System.out.println("  increasing: "+increasingMap);
		System.out.println("  decreasing: "+decreasingMap);
	}
}