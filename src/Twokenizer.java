/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
/*
 TweetMotif is licensed under the Apache License 2.0: 
 http://www.apache.org/licenses/LICENSE-2.0.html
 Copyright Brendan O'Connor, Michel Krieger, and David Ahn, 2009-2010.
*/

/*
 Scala verion of TweetMotif is licensed under the Apache License 2.0: 
 http://www.apache.org/licenses/LICENSE-2.0.html
 Copyright Jason Baldridge, and David Snyder, 2011.
*/

/*
 * A direct port to Java from Scala version of 
 * Twitter tokenizer at https://bitbucket.org/jasonbaldridge/twokenize
 * Original Python version TweetMotif can be found at https://github.com/brendano/tweetmotif
 * 
 * Author: Vinh Khuc (khuc@cse.ohio-state.edu)
 * July 2011
 */ 

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Twokenizer {
	
	// This pattern is different from the Scala version
	// '^' is added since Java Regex uses '^word$' for exact matching 'word' in the string 'word', 
	// not in the string 'abcword'
	private static Pattern Contractions = Pattern.compile("(?i)^(\\w+)(n't|'ve|'ll|'d|'re|'s|'m)$");
	private static Pattern Whitespace = Pattern.compile("\\s+");
	
	private static String punctChars = "['“\\\".?!,:;]";
	private static String punctSeq = punctChars + "+";
	private static String entity = "&(amp|lt|gt|quot);";
	
	//  URLs
	private static String urlStart1  = "(https?://|www\\.)";
	private static String commonTLDs = "(com|co\\.uk|org|net|info|ca|ly)";
	private static String urlStart2  = "[A-Za-z0-9\\.-]+?\\." + commonTLDs + "(?=[/ \\W])";
	private static String urlBody    = "[^ \\t\\r\\n<>]*?";
	private static String urlExtraCrapBeforeEnd = "(" + punctChars + "|"+entity + ")+?";
	private static String urlEnd     = "(\\.\\.+|[<>]|\\s|$)";
	private static String url        = "\\b(" + urlStart1 + "|" + urlStart2 + ")" + urlBody + "(?=(" + urlExtraCrapBeforeEnd + ")?" + urlEnd + ")";

	// Numeric
	private static String timeLike   = "\\d+:\\d+";
	private static String numNum     = "\\d+\\.\\d+";
	private static String numberWithCommas = "(\\d+,)+?\\d{3}" + "(?=([^,]|$))";

	// 'Smart Quotes' (http://en.wikipedia.org/wiki/Smart_quotes)
	private static String edgePunctChars    = "'\\\"“”‘’<>«»{}\\(\\)\\[\\]";
	private static String edgePunct    = "[" + edgePunctChars + "]";
	private static String notEdgePunct = "[a-zA-Z0-9]";
	private static Pattern EdgePunctLeft  = Pattern.compile("(\\s|^)(" + edgePunct + "+)(" + notEdgePunct + ")");
	private static Pattern EdgePunctRight = Pattern.compile("(" + notEdgePunct + ")(" + edgePunct + "+)(\\s|$)");    

	// Abbreviations
	private static String boundaryNotDot = "($|\\s|[“\\\"?!,:;]|" + entity + ")"; 
	private static String aa1  = "([A-Za-z]\\.){2,}(?=" + boundaryNotDot + ")";
	private static String aa2  = "[^A-Za-z]([A-Za-z]\\.){1,}[A-Za-z](?=" + boundaryNotDot + ")";
	private static String standardAbbreviations = "\\b([Mm]r|[Mm]rs|[Mm]s|[Dd]r|[Ss]r|[Jj]r|[Rr]ep|[Ss]en|[Ss]t)\\.";
	private static String arbitraryAbbrev = "(" + aa1 + "|" + aa2 + "|" + standardAbbreviations + ")";

	private static String separators  = "(--+|―)";
	private static String decorations = "[♫]+";
	private static String thingsThatSplitWords = "[^\\s\\.,]";
	private static String embeddedApostrophe = thingsThatSplitWords + "+'" + thingsThatSplitWords + "+";

	// Emoticons
	private static String normalEyes = "(?iu)[:=]";
	private static String wink = "[;]";
	private static String noseArea = "(|o|O|-)"; // rather tight precision, \\S might be reasonable...
	private static String happyMouths = "[D\\)\\]]";
	private static String sadMouths = "[\\(\\[]";
	private static String tongue = "[pP]";
	private static String otherMouths = "[doO/\\\\]"; // remove forward slash if http://'s aren't cleaned

	private static String emoticon = "(" + normalEyes + "|" + wink + ")" + noseArea + "(" + tongue + "|" + otherMouths + "|" + sadMouths + "|" + happyMouths + ")";
	
	// Delimiters
	private static Pattern Protected  = Pattern.compile(
	    "("
	    + emoticon + "|"
	    + url + "|"
	    + entity + "|"
	    + timeLike + "|"
	    + numNum + "|"
	    + numberWithCommas + "|"
	    + punctSeq + "|"
	    + arbitraryAbbrev + "|"
	    + separators + "|"
	    + decorations + "|"
	    + embeddedApostrophe + ")");
	
   // 'foo' => ' foo '
   public String splitEdgePunct(String input) {
	    Matcher splitLeftMatcher  = EdgePunctLeft.matcher(input);
	    String splitLeft = splitLeftMatcher.replaceAll("$1$2 $3");
	    
	    Matcher splitRightMatcher = EdgePunctRight.matcher(splitLeft);
	    return splitRightMatcher.replaceAll("$1 $2$3");
   }
	
   // "foo   bar" => "foo bar"
   public String squeezeWhitespace(String input) {
	   Matcher whitespaceMatcher = Whitespace.matcher(input);
	   return whitespaceMatcher.replaceAll(" ").trim();
   }
    
   // For special patterns
   public Vector<String> splitToken(String token) {
	   Matcher contractionsMatcher  = Contractions.matcher(token);
	   Vector<String> smallTokens = new Vector<String>();
	   
	   while (contractionsMatcher.find()) {
		   // There should be only two groups in a match for Contractors pattern
		   smallTokens.add(contractionsMatcher.group(1).trim());
		   smallTokens.add(contractionsMatcher.group(2).trim());
	   }
   
	   // if can't find a match, return the original token
	   if (smallTokens.size() == 0) {
		   smallTokens.add(token.trim());
	   }
	   
	   return smallTokens;
   }
   
   // simpleTokenize should be called after using squeezeWhitespace()
   public List<String> simpleTokenize(String text) {
	   
	   // Do the no-brainers first
	   String splitPunctText = splitEdgePunct(text);
	   int textLength = splitPunctText.length();
	   
	   // Find the matches for subsequences that should be protected,
	   // e.g. URLs, 1.0, U.N.K.L.E., 12:53
	   Matcher protectedMatcher = Protected.matcher(splitPunctText);
	   
	   // The spans of the "bads" should not be split.
	   // XXX: I do this since I couldn't find an equivalent method in Java which does 
	   // the same as Protected.findAllIn(splitPunctText).matchData.toList
	   // badSpans is a vector of indices
	   Vector<Integer> badSpans = new Vector<Integer>();
	   while (protectedMatcher.find()) {
		   int start = protectedMatcher.start();
		   int end = protectedMatcher.end();
		   // if this badSpan is not empty
		   if (start != end) {
			   badSpans.add(new Integer(start));
			   badSpans.add(new Integer(end));
		   }
	   }
	   
	   // Create a list of indices to create the "goods", which can be
	   // split. We are taking "bad" spans like 
	   //     List((2,5), (8,10)) 
	   // to create 
	   //    List(0, 2, 5, 8, 10, 12)
	   // where, e.g., "12" here would be the textLength
	   Vector<Integer> indices = new Vector<Integer>();
	   // add index 0
	   indices.add(new Integer(0));
	   // add indices from badSpans
	   indices.addAll(badSpans);
	   // add index length -1 
	   indices.add(new Integer(textLength));
	   
	   // XXX: calculate splitGoods directly without computing 'goods'
	   Vector<Vector<String>> splitGoods = new Vector<Vector<String>>();
	   for (int i=0; i<indices.size(); i+=2) {
		   String strGood = splitPunctText.substring(indices.get(i), indices.get(i+1));
		   Vector<String> splitGood = new Vector<String>(Arrays.asList(strGood.trim().split(" ")));
		   splitGoods.add(splitGood);
	   }

	   // Storing as Vector<String> 
	   Vector<String> bads = new Vector<String>();
	   for (int i=0; i<badSpans.size(); i+=2) {
		   String strBad = splitPunctText.substring(badSpans.get(i), badSpans.get(i+1));
		   bads.add(strBad);
	   }

	   //  Re-interpolate the 'good' and 'bad' Lists, ensuring that
	   //  additional tokens from last good item get included
	   Vector<String> zippedStr = new Vector<String>();
	   if (splitGoods.size() == bads.size()) {
		   for (int i=0; i<splitGoods.size(); i++) {
			   zippedStr.addAll(splitGoods.get(i));
			   zippedStr.add(bads.get(i));
		   } 
	   } else {
		   for (int i=0; i<splitGoods.size()-1; i++) {
			   zippedStr.addAll(splitGoods.get(i));
			   zippedStr.add(bads.get(i));
		   } 		   
		   // add the last element from 'splitGoods'
		   zippedStr.addAll(splitGoods.lastElement());
	   }
	   
	   // split based on special patterns (like contractions) and remove all tokens are empty
	   Vector<String> finalTokens = new Vector<String>();
	   for (String str: zippedStr) {
		   Vector<String> tokens = splitToken(str);
		   // only add non-empty tokens
		   for (String token: tokens) {
			   if (!token.isEmpty()) {
				   finalTokens.add(token);
			   }
		   }
	   }

	   return finalTokens;
   }
   
   // the twokenize method which filters out white spaces before using simpleTokenize() 
   public List<String> twokenize(String text) {
	   return simpleTokenize(squeezeWhitespace(text));
   }
   
   public static void main(String[] args) throws IOException {
	   Twokenizer twokenizer = new Twokenizer();
       BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
       System.out.print("> ");
       // Read user input
       String inputStr = br.readLine();
       while (!inputStr.equals("")) {
			List<String> tokens = twokenizer.twokenize(inputStr);
			for (String token: tokens) {
				System.out.print(token + " ");
			}
			System.out.print("\n> ");
			inputStr = br.readLine();
       }
       br.close();
   }
}