package InvertedIndex;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.Collections;
import java.util.Comparator;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

/**
 * @author Rahul Vemuri
 * email: rvemuri@emai.arizona.edu
 */

public class InvertedIndexBuilder {

	@SuppressWarnings("rawtypes")
	public static void main(String[] args) 
	{


		String FILE_PATH = "D:\\Eclipse Workspace\\IRassg1\\TestFiles\\file1.txt";
		String QUERY = "( treatment OR drug ) AND schizophrenia";

		System.out.print("Enter absolute file path:");
		FILE_PATH = System.console().readLine();
		
		System.out.print("Enter query [leave space before and after the parenthesis]:");
		QUERY = System.console().readLine();
		
		//read file path
		File file = new File (FILE_PATH);

		List<List<String>> postings = new ArrayList<>();;	// tokens X docID


		/*
		 * STEP 1: GENERATE POSTINGS
		 */

		//read the file line by line and process docs
		//Each line is a new doc and the starting word in a line is it's  docID
		try(BufferedReader br = new BufferedReader(new FileReader(file))) 
		{
			int termIndex = 0;
			int docID =0;

			//for each line
			for(String line; (line = br.readLine()) != null;)
			{
				// tokenize the line		    	
				StringTokenizer tokens = new StringTokenizer(line);

				// skip the first token and increment docID
				tokens.nextToken();
				docID++;

				// read the remaining tokens and assign the docid
				while(tokens.hasMoreTokens())
				{
					postings.add(Arrays.asList(tokens.nextToken(), Integer.toString(docID)));
					termIndex++;
				}

			}

		}
		catch (Exception e)
		{
			e.printStackTrace();
			return;
		}

		/*
		 * STEP 2: SORT THE POSTINGS
		 */

		sortListOnTerms(postings);

		/*
		 *  STEP 3: CREATE POSTING LIST + DETERMINE DOC FREQ & MERGE DUPLICATES
		 */

		//HashMap structure:  term -> doc_list
		HashMap<String, List> postingLists = new HashMap<String, List>();

		Iterator<List<String>> iterator = postings.iterator();

		String prevTerm = "";

		while(iterator.hasNext())
		{
			List<String> list = iterator.next();

			String currentTerm = list.get(0);

			//when we have read all the occurrences/duplicates of the prev term
			if(!prevTerm.equals(currentTerm))
			{
				//sort the posting list of the previous term (skip if first term)
				if(prevTerm != "")
					Collections.sort(postingLists.get(prevTerm));

				//put new term in hashmap
				List<String> ls = new ArrayList<String>();
				ls.add(list.get(1));
				postingLists.put(currentTerm, ls);

				//update the prev term
				prevTerm = currentTerm;
			}

			// if it is the same term with different docId
			else
			{
				//retrieve the list assoc with the curr term
				List tempList = postingLists.get(currentTerm);

				//add the docID to the list
				tempList.add(list.get(1));

				//replace the old list
				postingLists.put(currentTerm, tempList);
			}

		}

		/*
		 * STEP 4: EVALUATE THE QUERY
		 */

//		List<String> queryResult =  new ArrayList<>();
//		List<String> term1List =  new ArrayList<>();
//		List<String> term2List =  new ArrayList<>();
//
//		String[] queryTokens = QUERY.split("\\s+");
//
//		//check if terms are in the dictionary
//		if(!validateQueryTerms(postingLists, queryTokens))
//			return;
//
//		term1List = postingLists.get(queryTokens[0]);
//		term2List = postingLists.get(queryTokens[2]);
//
//		//Depending on the operator do the respective operations
//		if(queryTokens[1].equalsIgnoreCase("AND"))
//			queryResult = intersect(term1List, term2List);
//		else if(queryTokens[1].equalsIgnoreCase("OR"))
//			queryResult = union(term1List, term2List);
//		else
//		{
//			System.out.println("Unsupported operator: " + queryTokens[1]);
//			return;
//		}
		
		StringTokenizer queryTokens =  new StringTokenizer(QUERY);	//default delim is " \t\n\r\f"
		
		Stack<List<String>> operandStack = new Stack();
		Stack<String> operatorStack = new Stack();
		
		
		while(queryTokens.hasMoreTokens())
		{
			String currToken = queryTokens.nextToken();
			
			//if an operand is found (i.e. if the token is not an operator or parenthesis) push it to val stack if it is valid
			if(!(currToken.equals("AND") || currToken.equals("OR")) && !currToken.equals("(") && !currToken.equals(")"))
			{
				@SuppressWarnings("unchecked")
				List<String> termList = postingLists.get(currToken);
				if(termList != null)
					operandStack.push(termList);
				else
				{
					System.out.println("This term is not in the dictionary:" + currToken);
					System.exit(-1);
				}
			}
			
			//if it is a LEFT parenthesis, push it to opr stack
			else if(currToken.equals("("))
			{
				operatorStack.push(currToken);
			}
			
			//if it is a RIGHT parenthesis,
			else if(currToken.equals(")") && !operandStack.isEmpty())
			{
				while(!operatorStack.peek().equals("("))
				{
					String operator = operatorStack.pop();			//pop operator
					List<String> operandList1 = operandStack.pop();	//pop operand1
					List<String> operandList2 = operandStack.pop();	//pop operand2
					
					List<String> resultList = applyOperator(operator, operandList1, operandList2);	//apply operation
					
					operandStack.push(resultList);	//store result in stack
				}
				
				operatorStack.pop(); //pop the left parenthesis off the stack
			}
			
			//if it is an operator,
			else if(currToken.equals("AND") || currToken.equals("OR"))
			{												
				// if operator stack is empty or if the top element is a left parenthesis
				if(operatorStack.isEmpty() || operatorStack.peek().equals("("))
				{
					operatorStack.push(currToken); //push operator onto stack
				}
				
				else 
				{
					 // precedence: AND > OR
					 //if the currToken is OR, the top thing on the operator stack always has greater or equal precedence.. duh
					while(!operatorStack.isEmpty() && currToken.equals("OR"))	
					{
						String operator = operatorStack.pop();			//pop operator
						List<String> operandList1 = operandStack.pop();	//pop operand1
						List<String> operandList2 = operandStack.pop();	//pop operand2
						
						List<String> resultList = applyOperator(operator, operandList1, operandList2);	//apply operation
						
						operandStack.push(resultList);	//store result in stack
					}
					
					operatorStack.push(currToken);	//push operator onto stack
				}
			}
		}
		
		//after reading all the tokens, if the operator stack is not empty
		while(!operatorStack.isEmpty())
		{
			String operator = operatorStack.pop();			//pop operator
			List<String> operandList1 = operandStack.pop();	//pop operand1
			List<String> operandList2 = operandStack.pop();	//pop operand2
			
			List<String> resultList = applyOperator(operator, operandList1, operandList2);	//apply operation
			
			operandStack.push(resultList);	//store result in stack
		}
		
		//At this point (if the query is not invalid) the operator stack should contain only one value which is the result
		@SuppressWarnings("unused")
		List<String> queryResult = operandStack.pop();
		
		System.out.println("\n");
		
		for(String e : queryResult)
			System.out.println(e + ", ");
		
	}

	private static List<String> applyOperator(String operator, List<String> operandList1, List<String> operandList2) {

		if(operator.equals("AND"))
			return intersect(operandList1, operandList2);
		else if(operator.equals("OR"))
			return union(operandList1, operandList2);
		else
		{
			System.out.println("Unknown operator in stack :" + operator);
			System.exit(-1);
			return null;	//unreachable but the program would exit anyway
		}
	}

	private static List<String> union(List<String> term1DocIDList, List<String> term2DocIDList) {

		List<String> result = new ArrayList<>();

		while(!term1DocIDList.isEmpty() || !term2DocIDList.isEmpty())
		{
			
			if(term1DocIDList.isEmpty())
			{
				result.add(term2DocIDList.get(0));
				term2DocIDList.remove(0);				
			}
			
			else if(term2DocIDList.isEmpty())
			{
				result.add(term1DocIDList.get(0));
				term1DocIDList.remove(0);
				
			}

			else 
			{
				if(term1DocIDList.get(0).equals(term2DocIDList.get(0)))
				{
					result.add(term1DocIDList.get(0));
					
					term1DocIDList.remove(0);
					term2DocIDList.remove(0);
				}
				else if(Integer.parseInt(term1DocIDList.get(0)) < Integer.parseInt(term2DocIDList.get(0)))
				{
					result.add(term1DocIDList.get(0));
					term1DocIDList.remove(0);
				}
				else
				{
					result.add(term2DocIDList.get(0));
					term2DocIDList.remove(0);
				}
			}
								
		}
		
		return result;

	}

	private static List<String> intersect(List<String> term1DocIDList, List<String> term2DocIDList) {

		List<String> result = new ArrayList<String>();


		while(!term1DocIDList.isEmpty() && !term2DocIDList.isEmpty())
		{
			if(term1DocIDList.get(0).equals(term2DocIDList.get(0)))
			{
				result.add(term1DocIDList.get(0));				
				term1DocIDList.remove(0);
				term2DocIDList.remove(0);				
			}
			else if(Integer.parseInt(term1DocIDList.get(0)) < Integer.parseInt(term2DocIDList.get(0)))
				term1DocIDList.remove(0);
			else
				term2DocIDList.remove(0);
		}

		return result;
	}

	private static boolean validateQueryTerms(HashMap<String, List> postingLists, String[] queryTokens) {
		//check if the terms are actually in the dictionary
		for(int i = 0; i < queryTokens.length; i+=2)
		{
			if(!postingLists.containsKey(queryTokens[i]))
			{
				System.out.println(queryTokens[i] + " is not found in the dictionary");
				return false;
			}
		}

		return true;
	}

	private static void sortListOnTerms(List<List<String>> postings) {

		Collections.sort(postings, new Comparator<List<String>>() 
		{    
			@Override
			public int compare(List<String> a1, List<String> a2) 
			{
				return a1.get(0).compareTo(a2.get(0));
			}               
		});
	}
	
}
