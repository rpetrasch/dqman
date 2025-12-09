#include <stdio.h>

/*
 *Example for data types and conversion
 */
int main() {
   
  int num = 10;	// num is fixed as an integer.
  char *str = "5";  // str is fixed as a pointer to a string literal.
  char ch = '5';  // single char.
  int result1 = num + (int)str;
  int result2 = num + ch;  // implicit type conversion from char to int
  printf("%d %c %s %p %d %d %d\n", num, ch, str, (int)str, (int)str, result1, result2);

  char *result_str = (char *) result1;
  printf("%s", result_str);
  return 0;
}

