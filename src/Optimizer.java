public abstract class Optimizer		//Abstract optimisation for general goods.
{
	double Remainder, RatioA, RatioB, optA, optB;	//Global variables required for easier calculation/parsing.
	int Quant[] = new int[3];						//Rounded values for optA and optB.
	boolean Proceed, Exhaust = true; 				//optA and optB can return decimal (non-integer) values if true.
	
	protected Optimizer (boolean round)
	{
		Exhaust = round;
	}
	
	abstract String getInput (String question);		//UI for parameters
	
	abstract void showOutput (String message);		//How messages are displayed
	
	public void autorun (int mode)		//Automatically handle Q&A
	{
		if (mode == 1)	//BinComp
		{
			String items = getInput ("What kind of things would you like to compare?");
			int arrSize = getN (items);
			double[] prices = new double [arrSize];
			double[] utilities = new double [arrSize];
			int format = (int) getVar ("FUNCTION", items);
			String[] goods = getGoods (arrSize);
			double budget = getVar ("CONSTRAINT","MONEY");
			for (int i = 0; i < arrSize; i++)
			{
				prices[i] = getVar ("PRICE", goods[i]);
				utilities[i] = getVar ("UTILITY", goods[i]);
			}
			BinComp (format, goods, budget, prices, utilities);
		}
		else if (mode == 2)
		{
			String items = getInput ("What kind of things would you like to compare?");
			int arrSize = getN (items);
			double[] utilities = new double [arrSize];
			double[] mcosts = new double [arrSize];
			double[] tcosts = new double [arrSize];
			String[] goods = getGoods (arrSize);
			double maxm = getVar ("CONSTRAINT","MONEY");
			double maxt = getVar ("CONSTRAINT","TIME");
			for (int i = 0; i < arrSize; i++)
			{
				mcosts[i] = getVar ("MCOST", goods[i]);
				tcosts[i] = getVar ("TCOST", goods[i]);
				utilities[i] = getVar ("UTILITY", goods[i]);
			}
			SubSelect (goods, utilities, mcosts, tcosts, maxm, maxt);
		}
	}
	
    public int[] getResults()
    {
        return Quant;
    }
    
	void BinComp (int Format, String[] Good, double Budget, double[] Price, double[] Utility)	//Order a list of goods by value and let user decide which two to consider.
	{
		if (Format == 0) Format = 2;				//Assume "Balanced" by default.
		int arraySize = Price.length;				//Number of goods.
		int[] twoG = new int [3];					//The two goods that will be compared.
		double[] blank = new double [0];			//Syntax required by Rank method.
		int[] List = new int [arraySize];			//Ordered array.
		String[] Gab = new String [3];				//gA & gB.
		double[] Pab = new double [3];				//pA & pB.
		double[] Uab = new double [3];				//uA & uB.
		String top = "In order, these are your best choices (sorted by value for money): \n";
		List = this.Rank (Utility, Price, blank, Budget, -1.0);
		for (int i = 0; i < arraySize; i++)
			top += (i+1)+") "+ Good [List[i]] +" \n";
		showOutput(top);
		
		for (int i = 0; i < 2; i++)	//Gets the two goods for comparing.
		{
			while (true)
			{
				try
				{
					String Response = getInput ("Which two of these would you like to purchase? Enter the corresponding number.");
					twoG[i] = Integer.parseInt (Response);
					if (twoG[i] < 1 || twoG[i] > arraySize)
						throw new ArrayIndexOutOfBoundsException();
					
					if (i > 0 && twoG[i] == twoG[i-1])		//Duplicate.
						showOutput ("You've already entered that!");
					else 
					{
						twoG[i] -= 1;	//Arrays start at 0.
						break;
					}
				}
				catch (NumberFormatException NaN)
				{
					showOutput ("Please enter a (whole) number.");
				}
				catch (ArrayIndexOutOfBoundsException H)
				{
					showOutput ("Please enter one of the items on the list.");
				}
			}
			Gab[i] = Good [twoG[i]];
			Pab[i] = Price [twoG[i]];
			Uab[i] = Utility [twoG[i]];
		}
		this.HandleOutput (Gab, Exhaust, Format, Pab, Uab, Budget);		  //Will process the appropriate output depending on the acquired values.
	}
	
	int[] Rank (double[] U, double[] C1, double[] C2, double B1, double B2)	//Insertion sorting algorithm. Orders the goods by cost-benefit ratio.
	{
		int argc = Math.max ((Math.max (U.length, C1.length)), C2.length);
		int[] Order = new int [argc];
		double[] Lambda = new double [argc];
		
		if (U.length == argc)	//Normal case.
		{
			if (C2.length < argc && C1.length == argc)	//Single constraint.
			{
				if (B1 < 0 && B2 < 0)	//Ignore budget.
				{
					for (int x = 0; x < argc; x++)	//Initializer.
					{
						Order[x] = x;		
						Lambda[x] = U[x]/C1[x];	//Criteria.
					}
				}
				
				if (B1 < 0 && B2 > 0)	//Only account for one of the budgets.
				{
					for (int x = 0; x < argc; x++)	//Initializer.
					{
						Order[x] = x;		
						Lambda[x] = (U[x]/(C1[x]/B2));	//Criteria.
					}
				}
				
				if (B1 > 0 && B2 < 0)	//Only account for one of the budgets.
				{
					for (int x = 0; x < argc; x++)	//Initializer.
					{
						Order[x] = x;		
						Lambda[x] = (U[x]/(C1[x]/B1));	//Criteria.
					}
				}
				
				if (B1 > 0 && B2 > 0)	//Account for the sum of the budgets.
				{
					for (int x = 0; x < argc; x++)	//Initializer.
					{
						Order[x] = x;		
						Lambda[x] = (U[x]/C1[x])/(B1+B2);	//Criteria.
					}
				}
			}
			
			else if (C1.length == argc && C2.length == argc)	//Two constraints.
			{
				if (B1 < 0 && B2 < 0)	//Disregard budget.
				{
					for (int x = 0; x < argc; x++)	//Initializer.
					{
						Order[x] = x;		
						Lambda[x] = (U[x]/(C1[x]+C2[x]));	//Criteria.
					}
				}
				
				if (B1 < 0 && B2 > 0)	//Only take into account the first endowment.
				{
					for (int x = 0; x < argc; x++)	//Initializer.
					{
						Order[x] = x;	
						Lambda[x] = (U[x]/((C1[x]/B1)+C2[x]));	//Criteria.
					}
				}
				
				if (B1 > 0 && B2 < 0)	//Only take into account the second endowment.
				{
					for (int x = 0; x < argc; x++)	//Initializer.
					{
						Order[x] = x;	
						Lambda[x] = (U[x]/(C1[x]+(C2[x]/B2)));	//Criteria.
					}
				}
				
				if (B1 > 0 && B2 > 0)	//Account for both endowments.
				{
					for (int x = 0; x < argc; x++)	//Initializer.
					{
						Order[x] = x;	
						Lambda[x] = (U[x]/((C1[x]/B1)+(C2[x]/B2)));	//Criteria.
					}
				}
			}
			
			else if (C1.length < argc && C2.length == argc)	//Single constraint.
			{
				if (B1 < 0 && B2 < 0)	//Ignore budget.
				{
					for (int x = 0; x < argc; x++)	//Initializer.
					{
						Order[x] = x;		
						Lambda[x] = U[x]/C2[x];	//Criteria.
					}
				}
				
				if (B1 < 0 && B2 > 0)	//Only account for one of the budgets.
				{
					for (int x = 0; x < argc; x++)	//Initializer.
					{
						Order[x] = x;		
						Lambda[x] = (U[x]/(C2[x]/B2));	//Criteria.
					}
				}
				
				if (B1 > 0 && B2 < 0)	//Only account for one of the budgets.
				{
					for (int x = 0; x < argc; x++)	//Initializer.
					{
						Order[x] = x;		
						Lambda[x] = (U[x]/(C2[x]/B1));	//Criteria.
					}
				}
				
				if (B1 > 0 && B2 > 0)	//Account for the sum of the budgets.
				{
					for (int x = 0; x < argc; x++)	//Initializer.
					{
						Order[x] = x;		
						Lambda[x] = (U[x]/C2[x])/(B1+B2);	//Criteria.
					}
				}
			}
			
			else	//Unconstrained.
			{
				if (B1 > 0 && B2 > 0)	//Take into account both budgets.
				{
					for (int x = 0; x < argc; x++)	//Initializer.
					{
						Order[x] = x;	
						Lambda[x] = (U[x]/(B1+B2));	//Criteria.
					}
				}
				
				if (B1 > 0 && B2 < 0)	//Only account for one of the budgets.
				{
					for (int x = 0; x < argc; x++)	//Initializer.
					{
						Order[x] = x;	
						Lambda[x] = (U[x]/B1);	//Criteria.
					}
				}
				
				if (B1 < 0 && B2 > 0)	//Only account for one of the budgets.
				{
					for (int x = 0; x < argc; x++)	//Initializer.
					{
						Order[x] = x;	
						Lambda[x] = (U[x]/B2);	//Criteria.
					}
				}
				
				if (B1 < 0 && B2 < 0)	//Only base on utility alone.
				{
					for (int x = 0; x < argc; x++)	//Initializer.
					{
						Order[x] = x;	
						Lambda[x] = U[x];	//Criteria.
					}
				}
			}
		}
		
		else	//Just order by cost (highest to lowest).
		{
			if (C1.length > C2.length)	//Use the first cost.
			{
				if (B1 > 0)
				{
					if (B2 > 0)	//Take into account both budgets.
					{
						for (int x = 0; x < argc; x++)	//Initializer.
						{
							Order[x] = x;		
							Lambda[x] = C1[x]/(B1+B2);	//Criteria.
						}
					}
					if (B2 < 0)	//Only account for one of the budgets.
					{
						for (int x = 0; x < argc; x++)	//Initializer.
						{
							Order[x] = x;		
							Lambda[x] = C1[x]/B1;	//Criteria.
						}
					}
				}
				
				if (B1 < 0)
				{
					if (B2 > 0)	//Only account for one of the budgets.
					{
						for (int x = 0; x < argc; x++)	//Initializer.
						{
							Order[x] = x;		
							Lambda[x] = C1[x]/B2;	//Criteria.
						}
					}
					if (B2 < 0)	//Ignore budgets.
					{
						for (int x = 0; x < argc; x++)	//Initializer.
						{
							Order[x] = x;		
							Lambda[x] = C1[x];	//Criteria.
						}
					}
				}
			}
			
			if (C2.length > C1.length)	//Use the second cost.
			{
				if (B1 > 0)
				{
					if (B2 > 0)	//Take into account both budgets.
					{
						for (int x = 0; x < argc; x++)	//Initializer.
						{
							Order[x] = x;		
							Lambda[x] = C2[x]/(B1+B2);	//Criteria.
						}
					}
					if (B2 < 0)	//Only account for one of the budgets.
					{
						for (int x = 0; x < argc; x++)	//Initializer.
						{
							Order[x] = x;		
							Lambda[x] = C2[x]/B1;	//Criteria.
						}
					}
				}
				
				if (B1 < 0)
				{
					if (B2 > 0)	//Only account for one of the budgets.
					{
						for (int x = 0; x < argc; x++)	//Initializer.
						{
							Order[x] = x;		
							Lambda[x] = C2[x]/B2;	//Criteria.
						}
					}
					if (B2 < 0)	//Ignore budgets.
					{
						for (int x = 0; x < argc; x++)	//Initializer.
						{
							Order[x] = x;		
							Lambda[x] = C2[x];	//Criteria.
						}
					}
				}
			}
			
			if (C2.length == C1.length)	//Use both costs.
			{
				if (B1 > 0)
				{
					if (B2 > 0)	//Take into account both budgets.
					{
						for (int x = 0; x < argc; x++)	//Initializer.
						{
							Order[x] = x;		
							Lambda[x] = (C1[x]/B1)+(C2[x]/B2);	//Criteria.
						}
					}
					if (B2 < 0)	//Only account for one of the budgets.
					{
						for (int x = 0; x < argc; x++)	//Initializer.
						{
							Order[x] = x;		
							Lambda[x] = (C1[x]+C2[x])/B1;	//Criteria.
						}
					}
				}
				
				if (B1 < 0)
				{
					if (B2 > 0)	//Only account for one of the budgets.
					{
						for (int x = 0; x < argc; x++)	//Initializer.
						{
							Order[x] = x;		
							Lambda[x] = (C1[x]+C2[x])/B2;	//Criteria.
						}
					}
					if (B2 < 0)	//Ignore budgets.
					{
						for (int x = 0; x < argc; x++)	//Initializer.
						{
							Order[x] = x;		
							Lambda[x] = C1[x]+C2[x];	//Criteria.
						}
					}
				}
			}
		}
		
		for (int i = 0; i < argc; i++)	//Sorts Lambda.
		{
			int tmp = Order[i];
			int j = i;
			
			if (U.length == argc)	//Top to bottom.
			{
				while (j > 0 && Lambda[j-1] < Lambda[i])
				{
					Order[j] = Order[j-1]; 
					j--;
				}
			}
			else	//Bottom to top.
			{
				while (j > 0 && Lambda[j-1] > Lambda[i])
				{
					Order[j] = Order[j-1]; 
					j--;
				}
			}
			
			Order[j] = tmp;
		}
		return Order;	//Best to worst (descending order).
	}
	
	void HandleOutput (String[] G, boolean cZero, int fT, double[] P, double[] U, double C)		//Delivers the results.
	{
		if (fT == 2) Proceed = this.RationalChoice (G, P, U, C);
		else Proceed = this.CornerSolution (G, fT, P, U, C);
		
		if (Proceed)	//Result not already displayed in calculating function.
		{
			if (!cZero)	//Designed to account for goods where quantities are discrete; so have to round up or down.
			{
				Quant[0] = (int) Math.round (optA);
				if (((Quant[0] * P[0]) + (optB * P[1])) > C)	//A is rounded up instead of down, so correct this.
					Quant[0]--;
				Quant[1] = (int) Math.round (optB);
				if (((optA * P[0]) + (Quant[1] * P[1])) > C)	//B is rounded up instead of down, so correct this.
					Quant[1]--;
					
				if (fT == 3)	//In case of compliments. Have to check twice to ensure rounding is correct.
				{
					if ((Quant[0] * RatioA) > Quant[1]) Quant[0]--;
					if ((Quant[1] * RatioB) > Quant[0]) Quant[1]--;
					if ((Quant[0] * RatioA) > Quant[1]) Quant[0]--;
					if ((Quant[1] * RatioB) > Quant[0]) Quant[1]--;
				}
				
				if ((Quant[0] * P[0]) + (Quant[1] * P[1]) <= C)	//Final check.
					Remainder = C - (Quant[0] * P[0]) - (Quant[1] * P[1]);	//Calculate leftovers AFTER rounding to avoid spurious results.
				else	//Exception.
				{
					showOutput ("A rounding error occurred. Will use continuous values instead...");
					cZero = false;
				}
					
				if (Remainder > 0)	//Spare change.
				{
					if (Quant[0] > 1)
					{
						if (Quant[1] > 1)
							showOutput ("You should buy " + Quant[0] + " units of " + G[0] + " and " + Quant[1] + " units of " + G[1] + ". You will have " + Remainder + " of your budget unspent.");
						else if (Quant[1] == 1)
							showOutput ("You should buy " + Quant[0] + " units of " + G[0] + " and one " + G[1] + ". You will have " + Remainder + " of your budget unspent.");
						else if (Quant[1] == 0)
							showOutput ("You should just buy " + Quant[0] + " of " + G[0] + ". You will have " + Remainder + " of your budget unspent.");
					}
					
					else if (Quant[0] == 1)
					{
						if (Quant[1] > 1)
							showOutput ("You should buy one " + G[0] + " and " + Quant[1] + " units of " + G[1] + ". You will have " + Remainder + " of your budget unspent.");
						else if (Quant[1] == 1)
							showOutput ("You should buy one of each. You will have " + Remainder + " of your budget unspent.");
						else if (Quant[1] == 0)
							showOutput ("You should just buy one " + G[0] + ". You will have " + Remainder + " of your budget unspent.");
					}
					
					else if (Quant[0] == 0)
					{
						if (Quant[1] > 1)
							showOutput ("You should just buy " + Quant[1] + " of " + G[1] + ". You will have " + Remainder + " of your budget unspent.");
						else if (Quant[1] == 1)
							showOutput ("You should just buy one " + G[1] + ". You will have " + Remainder + " of your budget unspent.");
						else if (Quant[1] == 0)	//Something wrong.
							showOutput ("Don't buy anything! You will have " + Remainder + " of your budget unspent.");
					}
					
					else	//Backup scenario.
						showOutput ("You should buy " + Quant[0] + " of " + G[0] + " and " + Quant[1] + " of " + G[1] + ". You will have " + Remainder + " of your budget unspent.");
				}
				
				else if (Math.round(Remainder) < 0)	//Something went wrong.
					showOutput ("Gone over your budget! According to the numbers, you should buy " + Quant[0] + " of " + G[0] + " and " + Quant[1] + " units of " + G[1] + ".");
				
				else	//C exhausted anyway (Remainder == 0).
				{
					if (Quant[0] == 0)
					{
						if (Quant[1] == 0)
							showOutput ("You should buy neither of these.");
						else if (Quant[1] == 1)
							showOutput ("You should buy " + Quant[0] + " units of " + G[0] + " and one " + G[1] + ".");
						else if (Quant[1] > 1)
							showOutput ("You should only buy " + Quant[1] + " units of " + G[1] + ".");
					}

					else if (Quant[0] == 1)
					{
						if (Quant[1] == 0)
							showOutput ("You should only buy " + Quant[0] + " units of " + G[0] + ".");
						else if (Quant[1] == 1)
							showOutput ("You should buy one of each.");
						else if (Quant[1] > 1)
							showOutput ("You should buy " + Quant[1] + " units of " + G[1] + " and one " + G[0] + ".");
					}
					
					else if (Quant[0] > 1)
					{
						if (Quant[1] == 0)
							showOutput ("You should only buy " + Quant[1] + " units of " + G[1] + ".");
						else if (Quant[1] == 1)
							showOutput ("You should buy " + Quant[0] + " units of " + G[0] + " and one " + G[1] + ".");
						else if (Quant[1] > 1)
							showOutput ("You should buy " + Quant[0] + " of " + G[0] + " and " + Quant[1] + " units of " + G[1] + ".");
					}
				}
			}
					
			else	//To spend all C, quantities must be continuous so return exact values.
			{
				if (optA == 0 && optB != 0)
					showOutput ("You should buy " + optB + " of " + G[1] + ".");
				else if (optB == 0 && optA != 0)
					showOutput ("You should buy " + optA + " of " + G[0] + ".");
				else
					showOutput ("You should buy " + optA + " of " + G[0] + " and " + optB + " of " + G[1] + ".");
			}
		}
	}
	
	void SubSelect (String[] G, double[] U, double[] Pc, double[] tC , double Mo, double Ti)	//Returns the relevant goods the consumer should buy.
	{
		int Q = U.length;							//Number of items.
		String output = "You should buy: \n";		//The message that will be displayed.
		double mEXP = 0; 							//Running total of money spent.
		double tEXP = 0;							//Running total of time spent.
		int k = Q;									//Number of affordable items.
		//int[] N = new int[Q];						//Acts as a 'tracker' for the goods array, allowing for easy identification.
		double mREM;								//Remaining monetary budget.
		double tREM;								//Remaining temporal budget.
		int[] SL = this.Rank (U, Pc, tC, Mo, Ti);	//Sorts the items by optimality (best to worst).
		
		for (int tot = 0; tot < Q; tot++)	//Cost of buying one of everything.
		{
			mEXP += Pc[tot];
			tEXP += tC[tot];
		}
		
		while (mEXP > Mo && tEXP > Ti && k >= 0)	//Subtract the worst value goods until the best are affordable.
		{
			//N[SL[k]] = k;
			mEXP -= Pc[SL[Q-k]];
			tEXP -= tC[SL[Q-k]];
			k--;
		}
		mREM = Mo - mEXP;
		tREM = Ti - tEXP;
		for (int i = 0; i < k; i++)		//Display affordable goods in order.
			output += G[SL[i]]+" \n";

		showOutput (output + "You will have " + mREM + " of your budget left and " + tREM + " of your time unspent.");
	}
	
	private boolean RationalChoice (String[] G, double[] P, double[] U, double C)	//Calculates optimal values for well-balanced preferences.
	{
		//Utility function takes Cobb-Douglas form: U(A,B) = (A^uA)*(B^uB) with constraint C = pA*A + pB*B
		if (P[0] <= 0 || P[1] <= 0 || C <= 0 || U[0] <= 0 || U[1] <= 0)
		{
			showOutput ("Check your numbers - make sure they're all positive!");
			return false;	//Exception.
		}
		
		if (P[0] > C && P[1] > C)	//Can't afford either.
		{
			showOutput ("Your budget is too small, or you have expensive tastes!");
			return false;
		}
		
		if (P[0] > C && P[1] < C)	//A is unaffordable.
		{
			optB = C/P[1];
			showOutput ("Since you can only afford " + G[1] + ", you should buy " + optB + " units of it.");
			return false;
		}
		
		if (P[1] > C && P[0] < C)	//B is unaffordable.
		{
			optA = C/P[0];
			showOutput ("Since you can only afford " + G[0] + ", you should buy " + optA + " units of it.");
			return false;
		}
		
		if (P[0]+P[1] == C)	//Maximum qA = 1+(P[1]*((1-qB)/P[0])) and vice-versa. In other words, quantity of one of the goods is less than or equal to 1.
		{
			if ((U[0]/P[0]) == (U[1]/P[1]))	//Marginal Utility and Marginal Cost ratios are equal for both...
			{
				if (P[0] == P[1])		//...and one of each is exactly affordable.
				{
					showOutput ("Buy one of each!");
					return false;
				}
				if (P[0] > P[1]) //...but can't afford both, so just buy B.
				{
					optA = 0;
					optB = C/P[1];
				}
				if (P[1] > P[0])	//...but can't afford both, so just buy A.
				{
					optB = 0;
					optA = C/P[0];
				}
			}
			
			if ((U[0]/P[0]) > (U[1]/P[1]))	//A is better value...
			{
				if (P[0] == P[1])	//One of each is affordable, but consumer can buy exactly two of A instead.
				{
					showOutput ("You should just buy two units of " + G[0] + ".");
					return false;
				}
				if (P[0] > P[1])	//B will always be less than 1, so go for A.
				{
					optA = 1+(P[1]/P[0]);
					optB = 0;
				}
				if (P[0] < P[1])	//We require A < B, where B = 1.
				{
					optB = 1;
					optA = (C-P[1])/P[0];
				}
			}
			
			if ((U[1]/P[1]) > (U[0]/P[0]))	//B is better value...
			{
				if (P[0] == P[1])	//One of each is affordable, but consumer can buy exactly two of B instead.
				{
					showOutput ("You should just buy two units of " + G[1] + ".");
					return false;
				}
				if (P[1] > P[0])	//A will always be less than 1, so go for B.
				{
					optB = 1+(P[0]/P[1]);
					optA = 0;
				}
				if (P[0] < P[1])	//We require B < A, where A = 1.
				{
					optA = 1;
					optB = (C-P[0])/P[1];
				}
			}
		}
		
		if (P[0]+P[1] > C && P[0] < C && P[1] < C) //Can't have both...
		{
			if (P[0] > P[1])	//A more expensive...
			{
				if ((U[0]/P[0]) > (U[1]/P[1]))	//A is better value. So a mix of both.
				{
					optA = 1;
					optB = (C-(P[0]*optA))/P[1];
				}
				if ((U[1]/P[1]) > (U[0]/P[0]))	//B is better value AND cheaper. Only buy B.
				{
					optB = C/P[1];
					optA = 0;
				}
				if ((U[0]/P[0]) == (U[1]/P[1]))	//equally good value, so split.
				{
					optB = (C-P[0])/P[1];
					optA = (C-(P[1]*optB))/P[0];
				}
			}
			
			if (P[1] > P[0])	//B more expensive...
			{
				if ((U[1]/P[1]) > (U[0]/P[0]))	//B is better value. So a mix of both.
				{
					optB = 1;
					optA = (C-(P[1]*optB))/P[0];
				}
				if ((U[0]/P[0]) > (U[1]/P[1]))	//A is better value AND cheaper. Only buy A.
				{
					optA = C/P[0];
					optB = 0;
				}
				if ((U[1]/P[1]) == (U[0]/P[0]))	//equally good value, so split.
				{
					optA = (C-P[1])/P[0];
					optB = (C-(P[0]*optA))/P[1];
				}
			}
		}
		
		if (P[0] == C || P[1] == C)	//Maximum quantity of one of the goods is exactly 1.
		{
 			if (U[0] > U[1]) //A better than B.
			{
				if (P[0] == P[1])	//Both prices equal to budget, so choose the one with highest utility (A).
				{
					showOutput ("Go for " + G[0] + ".");
					return false;
				}
				if ((P[0] > P[1]) && ((U[0]/P[0]) > (U[1]/P[1])))	//P[1] smaller than budget, P[0] equal to budget, U[0] greater than U[1]. Marginal Utility / Marginal Cost ratio of A higher than B.
				{
					showOutput ("Go for " + G[0] + ".");
					return false;
				}
				if ((P[0] > P[1]) && ((U[0]/P[0]) < (U[1]/P[1])))	//P[1] smaller than budget, P[0] equal to budget, U[0] greater than U[1]. Marginal Utility / Marginal Cost ratio of B higher than A.
				{
					optA = 0;
					optB = C/P[1];
				}
				if (P[0] < P[1])	//P[1] equal to budget, P[0] smaller than budget, U[0] greater than U[1]. So buy as many of A as possible.
				{
					optB = 0;
					optA = C/P[0];
				}
			}
			
			if (U[1] > U[0]) //B better than A.
			{
				if (P[1] == P[0])	//Both prices equal to budget, so choose the one with highest utility (B).
				{
					showOutput ("Go for " + G[0] + ".");
					return false;
				}
				if ((P[1] > P[0]) && ((U[1]/P[1]) > (U[0]/P[0])))	//P[0] smaller than budget, P[1] equal to budget, U[1] greater than U[0]. Marginal Utility / Marginal Cost ratio of B higher than A.
				{
					showOutput ("Go for " + G[1] + ".");
					return false;
				}
				if ((P[1] > P[0]) && ((U[1]/P[1]) < (U[0]/P[0])))	//P[0] smaller than budget, P[1] equal to budget, U[1] greater than U[0]. Marginal Utility / Marginal Cost ratio of A higher than B.
				{
					optB = 0;
					optA = C/P[0];
				}
				if (P[1] < P[0])	//P[0] equal to budget, P[1] smaller than budget, U[1] greater than U[0]. So buy as many of B as possible.
				{
					optA = 0;
					optB = C/P[1];
				}
			}
			
			if (U[0] == U[1])	//Same Marginal Utility
			{
				if (P[0] == P[1])	//Marginal Utility to Marginal Cost ratio equal for both, but only one is affordable.
				{
					showOutput ("You should buy either of these, it makes no difference.");
					return false; 
				}
				if (P[0] > P[1])	//If P[0] > C, then B is affordable.
				{
					showOutput ("Go for " + G[1] + ".");
					return false;
				}
				if (P[1] > P[0])	//If P[1] > C, then A is affordable.
				{
					showOutput ("Go for " + G[0] + ".");
					return false;
				}
			}
		}

		else	//Derived using Lagrangian method.
		{
			optB = C/(((U[0]/U[1])+1)*P[1]);
			optA = (U[0]*optB*P[1])/(P[0]*U[1]);
			/*optA = (U[0]/(U[0]+U[1]))*(C/P[0]);
			optB = (C-(P[0]*optA))/P[1];*/
		}
		return true;
	}
	
	private boolean CornerSolution (String[] G, int fT, double[] P, double[] U, double C)	//Linear preferences
	{
		if (P[0] > C && P[1] > C)	//Can't afford either.
		{
			showOutput ("Your budget is too small, or you have expensive tastes!");
			return false;
		}
		
		if (fT == 1)	//Substitute goods
		{
			if (P[0] > C && P[1] < C)	//A is unaffordable.
			{
				optB = C/P[1];
				showOutput ("Since you can only afford " + G[1] + ", you should buy " + optB + " units of it, or reconsider your options.");
				return false;
			}
			
			if (P[1] > C && P[0] < C)	//B is unaffordable.
			{
				optA = C/P[0];
				showOutput ("Since you can only afford " + G[0] + ", you should buy " + optA + " units of it, or reconsider your options.");
				return false;
			}
			
			if ((U[0]/P[0]) > (U[1]/P[1]))	//A better value than B.
			{
				optA = C/P[0];
				optB = 0;
			}
			else if ((U[1]/P[1]) > (U[0]/P[0]))	//B better value than A.
			{
				optB = C/P[1];
				optA = 0;
			}
			else if ((U[0]/P[0]) == (U[1]/P[1]))	//Infinitely many solutions.
			{
				showOutput ("You should buy any affordable combination - it doesn't matter which or how many of each good you choose in this case.");
				return false;
			}
		}
		
		if (fT == 3)	//Leontief utility function.
		{
			if (P[0] > C || P[1] > C)	//Automatically failed optimisation.
			{
				showOutput ("Your budget is too small, or you have expensive tastes!");
				return false;
			}
			
			while (true)
			{
				try
				{
					String Response = getInput ("How many units of " + G[0] + " do you need for every " + G[1] + "?");
					if (Response == null)
						System.exit(0);
					RatioA = Double.parseDouble (Response);	//optB = RatioA * Quant[0].
					RatioB = 1/RatioA;
					break;
				}
				catch (NumberFormatException NaN)
				{
					showOutput ("Please enter a number.");
				}
			}
			optA = C/(P[0]+(RatioA*P[1]));
			optB = optA*RatioA;			//(RatioA*C)/(P[0]+(RatioA*P[1]));
			
			if (P[0]*RatioA + P[1] > C)	//Automatically failed optimisation.
			{
				showOutput ("You can't afford this bundle of goods");
				return false;
			}
		}
		return true;
	}
	
	String[] getGoods (int argc)	//Finds the names of the goods.
	{
		String Response;	//Input buffer.
		String[] Products = new String [argc];	//Declare the goods array.
		
		for (int i = 0; i < argc; i++)	//Ask user to enter all goods.
		{
			while (true)
			{
				String number = ""+(i+1);
				if (number.endsWith("1"))
					Response = getInput ("Please enter the "+ (i+1)+"st" +" product you'd like to compare.");
				else if (number.endsWith("2"))
					Response = getInput ("Please enter the "+ (i+1)+"nd" +" product you'd like to compare.");
				else if (number.endsWith("3"))
					Response = getInput ("Please enter the "+ (i+1)+"rd" +" product you'd like to compare.");
				else
					Response = getInput ("Please enter the "+ (i+1)+"th" +" product you'd like to compare.");
				
				if (Response == null)
					System.exit(0);
				else if (Response.length() == 0)
					showOutput ("Please enter a product name.");
				else if (i > 0 && Products[i] == Products[i-1])
					showOutput ("Please enter a different name to avoid confusion.");
				else	//Success.
				{
					Products[i] = Response;
					break;
				}
			}
		}
		return Products;
	}
	
	double getVar (String Type, String Product)	//Obtains the necessary parameters through user input.
	{
		Type = Type.toUpperCase();
		String Response, Name;
		String IA = "a ";	//Indefinite article
		
		if (Product != null)
		{
			Name = Product.toUpperCase();
			if (Name.startsWith ("A") || Name.startsWith ("E") || Name.startsWith ("I") || Name.startsWith ("O") || Name.startsWith ("U") || Name.startsWith ("8"))
				IA = "an ";
		}
		
		if (Type == "FUNCTION")
		{
			Response = getInput ("Are these goods\n 1)Substitutes?\n 2)Balanced?\n 3)Compliments?");
			if (Response.toUpperCase() == "SUBSTITUTES" || Response == "1")
				return 1;
			else if (Response.toUpperCase() == "BALANCED" || Response == "2")
				return 2;
			else if (Response.toUpperCase() == "COMPLIMENTS" || Response == "3")
				return 3;
			else
				return 0;
		}
		
		if (Type == "UTILITY")	//Uses revealed preference theory.
		{
			double utility;
			while (true)
			{
				try
				{
					Response = getInput ("What is the most you'd be willing to pay for " + IA + Product + "?");
					if (Response == null)
						System.exit(0);
						
					utility = Double.parseDouble (Response);
					if (utility == 0)
						showOutput ("You want it for free? Come on, be realistic. Is it really that worthless to you?!?");
					else if (utility < 0)
						showOutput ("You can't possibly expect to get PAID to buy it. Be reasonable!");
					else break;
				}
				catch (NumberFormatException NaN)
				{
					showOutput ("Please enter a number.");
				}
			}
			return utility;
		}
		
		if (Type == "MCOST" || Type == "MONEY" || Type == "PRICE")	//Returns the per unit monetary cost of the specified item.
		{
			double price;
			while (true)
			{
				try
				{
					Response = getInput ("How much does " + IA + Product + " cost?");
					if (Response == null)
						System.exit(0);
						
					price = Double.parseDouble (Response);
					if (price > 0)
						break;
					else
						showOutput ("Price has to be positive!");
				}
				catch (NumberFormatException NaN)
				{
					showOutput ("Please enter a number.");
				}
			}
			return price;
		}
		
		if (Type == "TCOST" || Type == "TIME")	//Returns the per unit temporal cost of the specified item.
		{
			double price;
			while (true)
			{
				try
				{
					Response = getInput ("How much time does each unit of " + Product + " use up?");
					if (Response == null)
						System.exit(0);
						
					price = Double.parseDouble (Response);
					if (price > 0)
						break;
					else
						showOutput ("Time has to be positive!");
				}
				catch (NumberFormatException NaN)
				{
					showOutput ("Please enter a number.");
				}
			}
			return price;
		}
		
		if (Type == "CONSTRAINT")	//Returns the maximum permitted expenditure.
		{
			double constraint;
			while (true)
			{
				if (Product == "MONEY" || Product == null)	//Total monetary budget.
				{
					try
					{
						Response = getInput ("What is your budget?");
						if (Response == null)
							System.exit(0);
							
						constraint = Double.parseDouble (Response);
						if (constraint <= 0)
							showOutput ("Cheapskate!.");
						else break;
					}
						
					catch (NumberFormatException NaN)
					{
						showOutput ("Please enter a number.");
					}
				}
			
				if (Product == "TIME")	//Total temporal budget.
				{
					try
					{
						Response = getInput ("How much time do you have to spend (in hours)?");
						if (Response == null)
							System.exit(0);
							
						constraint = Double.parseDouble (Response);
						if (constraint <= 0)
							showOutput ("Why bother then?!?.");
						else break;
					}
						
					catch (NumberFormatException NaN)
					{
						showOutput ("Please enter a number.");
					}
				}
			}
			return constraint;
		}
		else return -1;	//Error occurred.
	}
	
	int getN (String Products)	//Gets number of goods to be compared.
	{
		String Response;	//Input buffer
		int nG;
		while (true)
		{
			try
			{
				Response = getInput ("How many " + Products + " would you like to compare?");
				if (Response == null)
					System.exit(0);
				
				nG = Integer.parseInt (Response);
				if (nG < 0)
					throw new NegativeArraySizeException();
				else if (nG < 2)	//Something went wrong.
					showOutput ("Can't have less than two " + Products + " to compare!");
				else break;
			}
			catch (NumberFormatException NaN)
			{
				showOutput ("Please enter a whole number.");
			}
			catch (NegativeArraySizeException NaS)
			{
				showOutput ("Please enter a positive value.");
			}
		}
		return nG;
	}
}
