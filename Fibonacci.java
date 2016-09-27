import java.io.*;

//commented out sections are  one to make fibonacci4 able to run numbers
//(n) that are not powers of two
public class Fibonacci {
	 
	public static void main(String[] args){
		
		System.out.print("Enter a number:");
		BufferedReader br= new BufferedReader(new 
				InputStreamReader(System.in));
		int n=0;
		try{n= Integer.parseInt(br.readLine());}
		catch(IOException e){
			System.out.println("IO Exception");
			System.exit(1);
		}
		long starttime = System.nanoTime();
		System.out.println("Fibonacci1: " + fibonacci1(n));
		long endtime =  System.nanoTime();
		long total = endtime - starttime;
		System.out.println("Execution time: " + total);
		
		starttime = System.nanoTime();
		System.out.println("Fibonacci2: " + fibonacci2(n));
		endtime =  System.nanoTime();
		total = endtime - starttime;
		System.out.println("Execution time: " + total);
		
		starttime = System.nanoTime();
		System.out.println("Fibonacci3: " + fibonacci3(n));
		endtime =  System.nanoTime();
		total = endtime - starttime;
		System.out.println("Execution time: " + total);
		
		starttime = System.nanoTime();
		System.out.println("Fibonacci4: " + fibonacci4(n));
		endtime =  System.nanoTime();
		total = endtime - starttime;
		System.out.println("Execution time: " + total);

	}
	
	public static int[][] mult2by2(int[][] a, int[][] b){
		
		int arrayC[][] = new int[2][2];
		arrayC[0][0] = a[0][0]*b[0][0] + a[0][1]*b[1][0];
		arrayC[0][1] = a[0][0]*b[0][1] + a[0][1]*b[1][1];
		arrayC[1][0] = a[1][0]*b[0][0] + a[1][1]*b[1][0];
		arrayC[1][1] = a[1][0]*b[0][1] + a[1][1]*b[1][1];
		return arrayC;
	}

	public static int fibonacci1(int n){
		if (n==0) return 0;
		if (n==1) return 1;
		return fibonacci1(n-1) + fibonacci1(n-2);
	}
	
	public static int fibonacci2(int n){
		
		if (n==0) return 0;
		int array[] = new int[n+1];
		array[0] = 0;
		array[1] = 1;
		for (int i=2; i<= n; i++){
			array[i] = array[i-1] + array[i-2];
		}
		return array[n];
	}
	
	public static int fibonacci3(int n){
		
		if (n==0) return 0;
		if (n==1) return 1;
		
		int arrayA[][] = new int[2][2];
		arrayA[0][0]= 0;
		arrayA[0][1]= 1;
		arrayA[1][0]= 1;
		arrayA[1][1]= 1;
		int arrayB[][]= new int[2][2];
		arrayB[0][0]= 0;
		arrayB[0][1]= 1;
		arrayB[1][0]= 1;
		arrayB[1][1]= 0;
		
		
		for(int i=1; i<n; i++){
			arrayB = mult2by2(arrayB, arrayA);
		}
		
		return arrayB[0][1];
	}
	
	public static int fibonacci4(int n){
		
		if (n==0) return 0;
		if (n==1) return 1;
		
		int arrayA[][] = new int[2][2];
		arrayA[0][0]= 0;
		arrayA[0][1]= 1;
		arrayA[1][0]= 1;
		arrayA[1][1]= 1;
		
		/*int total = 0;
		int binary[] = new int[n];
		int num = n;
		int count = 0;
		int powerof = 0;
		while (num != 0)
	    {
	        count++;
	        binary[count] = num % 2;	//converting to binary
	        num = num / 2;
	    }*/
		
//		for(int j=binary.length; j < 0; j--){  //runs through the array holding binarys numbers, and only runs if 1
//			if(binary[j] == 1){
//				int number = (int)Math.pow(2, powerof);
		for(int i=1; i<= Math.log(n)/Math.log(2); i++){
			arrayA = mult2by2(arrayA,arrayA);
//				powerof++;						
//				}
			}
//		total += arrayA[0][1];	
		return arrayA[0][1];
		
//		}return total;
	}
}
