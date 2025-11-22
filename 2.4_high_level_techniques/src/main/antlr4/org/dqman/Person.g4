grammar Person;

// --- PARSER RULES ---
person : PERSON LBRACE field+ RBRACE;

field  : NAME COLON STRING
       | FIRSTNAME COLON STRING
       | BIRTHDATE COLON DATE
       | NATIONALITY COLON STRING
       ;

// --- LEXER RULES ---
// Define all keywords and symbols as explicit lexer rules.
PERSON      : 'Person';
NAME        : 'name';
FIRSTNAME   : 'firstname';
BIRTHDATE   : 'birthdate';
NATIONALITY : 'nationality';

LBRACE : '{';
RBRACE : '}';
COLON  : ':';

DATE   : [0-9][0-9][0-9][0-9] '-' [0-9][0-9] '-' [0-9][0-9];
STRING : '"' ~["]* '"';
WS     : [ \t\r\n]+ -> skip;
