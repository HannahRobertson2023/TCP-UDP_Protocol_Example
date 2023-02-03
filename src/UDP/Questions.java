package UDP;
import java.util.Random;

public class Questions {
	static int quesNum;
	
	public static String getQuestion() {
		Random rand = new Random();
		quesNum = rand.nextInt(100);
		switch (quesNum % 20) {
		case 0:
			return "Who was the first President of the United States?\n(First and last name)";
		case 1:
			return "What is the capital of Maryland?";
		case 2: 
			return "What is 2 + 4?";
		case 3:
			return "What U.S. state is referred to as the Golden State?";
		case 4:
			return "What is 5 * 4?";
		case 5: 
			return "What is the capital of Utah?";
		case 6:
			return "What is 6 + 5?";
		case 7: 
			return "What cardinal direction does the sun generally set in?";
		case 8:
			return "What is the name of the ocean between the North America and Asia?";
		case 9:
			return "Who was the second president of the United States?\n(First and last name)";
		case 10: 
			return "In what city is ASU Polytechnic located?";
		case 11: 
			return "What is the capital of England?";
		case 12:
			return "What is 90 - 10?";
		case 13:
			return "What is the name of the ocean between the Americas and the Old World?";
		case 14:
			return "Who was the third President of the United States?\n(First and last name)";
		case 15:
			return "What is the color of the sky?";
		case 16:
			return "What animal is the dog descended from?";
		case 17:
			return "What is the capital of Japan?";
		case 18:
			return "What is the capital of Egypt?";
		case 19:
			return "What is 6 / 6?";
		default:
			return "What is 1 + 1?";
		}
	}
	public static String getAnswer() {
		switch (quesNum % 20) {
		case 0:
			return "George Washington";
		case 1:
			return "Baltimore";
		case 2: 
			return "6";
		case 3:
			return "California";
		case 4:
			return "20";
		case 5: 
			return "Salt Lake City";
		case 6:
			return "11";
		case 7: 
			return "West";
		case 8:
			return "Pacific";
		case 9:
			return "Thomas Jefferson";
		case 10: 
			return "Mesa";
		case 11: 
			return "London";
		case 12:
			return "80";
		case 13:
			return "Atlantic";
		case 14:
			return "John Adams";
		case 15:
			return "Blue";
		case 16:
			return "Wolf";
		case 17:
			return "Tokyo";
		case 18:
			return "Cairo";
		case 19:
			return "1";
		default:
			return "2";
		}
	}
}
