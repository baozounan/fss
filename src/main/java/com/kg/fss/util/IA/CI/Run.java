package com.kg.fss.util.IA.CI;

import com.kg.fss.util.IA.Graph.Graph;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

public class Run {
	public static int randomInt(int start, int end) {//����[start, end]֮��������
		Random random = new Random();
		int s = random.nextInt(end)%(end-start+1) + start;
		return s;
	}

	public static double randomDouble(double start, double end) {
		//�������������
		double result = start + Math.random() * (end - start);
		return result;
	}

	public int random_node(double capacity, Graph physic, ArrayList<Integer> result){
		//���ѡȡ����ڵ㣬����������ڵ��cpu������������ڵ��cpu�����Ҹ�����ڵ�û�б�ѡȡӳ���
		int physic_node = randomInt(0, physic.nodes.length - 1);

		//������ڵ�������������ڵ����Դ���߸�����ڵ��Ѿ���ѡ�й�,�򲻶�ѭ��������Լ����
		while(capacity > physic.nodes[physic_node].getCapacity()
				|| result.contains(physic_node)){
			physic_node = randomInt(0, physic.nodes.length - 1);
		}
		return physic_node;
	}

	public ArrayList<Integer> random_solution(Graph virtual, Graph physic){//����ӳ���ϵ
		ArrayList<Integer> result = new ArrayList<Integer>();
		for(int i = 0; i < virtual.nodes.length; i++){
			//��Ӧ��ϵ,����Ϊ0����������ڵ�Ϊ0����ֵ����ӳ�䵽�ĸ�����ڵ���
			result.add(random_node(virtual.nodes[i].getCapacity(), physic, result));
		}
		return result;//����ӳ����
	}

	public int object_function(int hop, int virtual_link_bandwidth){//ӳ�䵽��������·�ɱ�
		return hop * virtual_link_bandwidth;
	}

	public void evaluate(ArrayList<Po> pop, Graph physic, Graph virtual){//�����ռ������н��ֵ��
		//������������Ľڵ������������ϵĽڵ��ӳ���ϵ�����α�������������·����������������·�õ���������·����������ڵ�
		//����ӳ���ϵ�õ�ӳ�䵽���������ϵ���������ڵ㣬�õϽ�˹�����㷨�õ���������ڵ�����·����
		//������·���ĳɱ�Ϊ����*������·�����ý��ֵΪ��������·���ĳɱ�
		//����������·���ϴ�������������ӳ�䷽��(��)������,�øý��ֵΪ���ֵ

		for(int i = 0; i < pop.size(); i++){
			ArrayList<Integer> result = pop.get(i).getSolution();//�ڵ�ӳ���ϵ
			int cost = 0;
			int panduan = 0;
			for(int j = 0; j < virtual.links.length; j++){
				int left = virtual.links[j].getLeftID();
				int right = virtual.links[j].getRightID();
				int physic_left = result.get(left);
				int physic_right = result.get(right);
				int[] path = physic.dijkstra(physic_left, physic_right,
						virtual.links[j].getBandWidth());
				if(path == null){
					pop.get(i).setCost(Integer.MAX_VALUE);//���ַ����Ͳ�Ҫ��
					panduan = 1;
					break;
				}
				//�ɱ��������������Դ���
				cost += object_function(path.length - 1, virtual.links[j].getBandWidth());
			}
			if(panduan == 0){
				pop.get(i).setCost(cost);
			}
			physic.refreshMatrix();//����

		}
	}

	public double num_clones(int size, double clone_factor){//����ÿ������Ҫ��¡������
		return Math.floor(size * clone_factor);//����Math.floor(1.4)=1.0
	}

	public void calculate_affinity(ArrayList<Po> pop){//�����׺Ͷ�
		//��������׺Ͷ��Ǹ����ĵ��еĹ�ʽ��
		//���ȼ����ռ������ֵ-��Сֵ=range�����range==0,�����׺Ͷ�Ϊ0������ý���׺Ͷ�=1-(���ֵ/range)
		Collections.sort(pop, new SortByCost());
		int range = pop.get(pop.size() - 1).getCost() - pop.get(0).getCost();
		//System.out.println("last+" + pop.get(pop.size() - 1).getCost() + "begin" + pop.get(0).getCost());
		//System.out.println("range+" + range + "\n");
		if(range == 0){
			for(int i = 0; i < pop.size(); i++) {
				pop.get(i).setAffinity(1.0);
			}
		}else{
			for(int i = 0; i < pop.size(); i++) {
				pop.get(i).setAffinity(1.0 - (double)(pop.get(i).getCost() / range));
			}
		}
	}

	public double calculate_mutation_rate(Po po, double mutate_factor) {//�������ֵ
		//����ֵΪe^(�׺Ͷ�*��������)
		return Math.exp(po.getAffinity() * mutate_factor);
	}

	public ArrayList<Integer> point_mutation(ArrayList<Integer> solution, double m_rate,
											 Graph virtual, Graph physic){//�Խ���п�¡����
		ArrayList<Integer> content = new ArrayList<Integer>();
		for(int i = 0; i < solution.size(); i++){
			content.add(solution.get(i));
		}
		for(int i = 0; i < content.size(); i++){
			if(randomDouble(0.0, 1.0) < m_rate){//�Խ��е�ÿ��ֵ�����[0.0,1.0]���������һ����С�ڱ���ֵ
				//��Ը�ֵ���б��죬���������ѡȡ����ڵ����ӳ��
				content.set(i, random_node(virtual.nodes[i].getCapacity(), physic, content));
			}
		}
		return content;
	}

	public ArrayList<Po> clone_and_hypermutate(ArrayList<Po> pop, double clone_factor,
											   double mutate_factor, Graph virtual, Graph physic){//�Խ�ռ�pop�еĽ���п�¡����
		ArrayList<Po> clones = new ArrayList<Po>();
		double number_clones = num_clones(pop.size(), clone_factor);//����ÿһ����Ҫ���ɶ��ٸ���¡�Ľ�
		calculate_affinity(pop);//����ÿ������׺Ͷ�
		for(int i = 0; i < pop.size(); i++){
			double m_rate = calculate_mutation_rate(pop.get(i), mutate_factor);//����ÿ����ı���ֵ
			for(double j = 0; j < number_clones; j++){
				Po clone = new Po();
				clone.setSolution(point_mutation(pop.get(i).getSolution(), m_rate, virtual,
						physic));//�Խ���п�¡����
				clones.add(clone);
			}
		}

		evaluate(clones, physic, virtual);
		return clones;
	}

	public ArrayList<Po> combi(ArrayList<Po> pop, ArrayList<Po> clones){//��ռ�Ľ��
		for(int i = 0; i < pop.size(); i++) {
			clones.add(pop.get(i));
		}
		return clones;
	}

	public ArrayList<Po> first_pop_size(ArrayList<Po> combination, int pop_size){//ѡȡ��ǰpop_size����
		ArrayList<Po> child = new ArrayList<Po>();
		for(int i = 0; i < pop_size; i++) {
			child.add(combination.get(i));
		}
		return child;
	}

	public ArrayList<Po> random_insertion(ArrayList<Po> pop, int num_rand, int pop_size
			, Graph virtual, Graph physic){//��������⣬�ϲ�ԭʼ��ռ�pop��ѡȡ��ǰpop_size���ŵĽ�
		if(num_rand == 0){
			return pop;
		}
		ArrayList<Po> child = new ArrayList<Po>();
		for(int i = 0; i < num_rand; i++){
			Po po = new Po();
			po.setSolution(random_solution(virtual, physic));//���������
			child.add(po);
		}
		evaluate(child, physic, virtual);//���㿪����
		physic.refreshMatrix();
		ArrayList<Po> combination = combi(pop, child);
		Collections.sort(combination, new SortByCost());//��֤����û��
		pop = first_pop_size(combination, pop_size);
		//delete_null(pop);
		return pop;

	}

	public Po search(Graph physic, Graph virtual, int max_gens, int pop_size,
					 double clone_factor, int num_rand, double mutate_factor){
		ArrayList<Po> pop = new ArrayList<Po>();
		for(int i = 0; i < pop_size; i++){//���ɳ�ʼ��
			Po po = new Po();
			po.setSolution(random_solution(virtual, physic));//���������
			pop.add(po);
		}
		evaluate(pop, physic, virtual);//���㿪����
		physic.refreshMatrix();
		Collections.sort(pop, new SortByCost());
		//delete_null(pop);//ȥ�������ϸ�ķ�����
		Po best = pop.get(0);
		System.out.println("best" + pop.get(0).getCost());
		for(int i = 0; i < max_gens; i++){
			ArrayList<Po> clones = clone_and_hypermutate(pop, clone_factor,
					mutate_factor, virtual, physic);//�Խ���п�¡�����죬���ɽ�ռ�clones
			evaluate(clones, physic, virtual);
			calculate_affinity(clones);//���㿪��
			physic.refreshMatrix();
			ArrayList<Po> combination = combi(pop, clones);//ԭʼ����������
			Collections.sort(combination, new SortByCost());//����
			pop = first_pop_size(combination, pop_size);//ѡȡǰpop_size���ŵĽ�
			//delete_null(pop);
			//����num_rand������⣬��ԭ�ȵĽ�ϲ�ѡȡ��ǰpop_size���ŵĽ�
			pop = random_insertion(pop, num_rand, pop_size, virtual,physic);
			pop.add(best);//�����һ��ѭ�������Ž�
			Collections.sort(pop, new SortByCost());//����
			pop = first_pop_size(combination, pop_size);//ѡȡǰpop_size���ŵĽ�
			//delete_null(pop);
			best = pop.get(0);
			System.out.println("best-Affinity...........................");
			System.out.println(best.getAffinity());
			System.out.println("best-cost...........................");
			System.out.println(best.getCost());
		}
		Collections.sort(pop, new SortByCost());
		return pop.get(0);
	}

	public void delete_null(ArrayList<Po> pop){
		while(pop.size() > 0){
			if(pop.get(pop.size() - 1).getCost() == Integer.MAX_VALUE){
				pop.remove(pop.size() - 1);
			}else{
				break;
			}
		}
	}


	public static void main(String[] args) {
		System.out.println(System.getProperty("user.dir").toString());
		Run test = new Run();
		Graph physic=new Graph("\\src\\main\\java\\com\\kg\\fss\\util\\IA\\P1.txt",true);//55-103
		Graph virtual=new Graph("\\src\\main\\java\\com\\kg\\fss\\util\\IA\\V1.txt",false);//14-21
        /*for(int i = 0; i < physic.links.length; i++){
			System.out.println(physic.links[i].getBandWidth());
		}
		System.out.println("............");
		for(int i = 0; i < virtual.links.length; i++){
			System.out.println(virtual.links[i].getBandWidth());
		}*/
		//����ڵ�0������ȡֵ0-54
		physic.nodes[0].getCapacity();
		//����ڵ�0������ȡֵ0-13
		virtual.nodes[0].getCapacity();

		//��·ӳ�䣬���·������·��ΪNULL��������ڽӾ���
		int[] path=physic.dijkstra(1, 30, 2000);
		//�ڽӾ���ԭ
		physic.refreshMatrix();

		//������·�ڵ�
		virtual.links[0].getLeftID();
		virtual.links[0].getRightID();

		//������·����
		virtual.links[0].getBandWidth();

		int max_gens = 100;//����20��
		int pop_size = 100;//��ʼ��������
		double clone_factor = 0.2;//��¡����
		int num_rand = 2;//������ɿ��������
		double mutate_factor = -2.5;//��������
		Po result = test.search(physic, virtual, max_gens, pop_size, clone_factor, num_rand, mutate_factor);
		System.out.println("final: " + result.getCost());
	}
}
class SortByCost implements Comparator{
	public int compare(Object o1, Object o2) {
		Po s1 = (Po) o1;
		Po s2 = (Po) o2;
		if(s1.getCost() > s2.getCost()) {
			return 1;
		}else if(s1.getCost() < s2.getCost()) {
			return -1;
		}
		return 0;//���ʱҪ����0����Ϊֵ�ǳ���ȷ������ֵ�ǳ�Сʱ����Ϊ��0����Ҫ����0������ᱨ��
	}
}
