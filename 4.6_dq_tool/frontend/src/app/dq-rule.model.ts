export interface DqRule {
    id?: number;
    name: string;
    description: string;
    ruleType: string;
    ruleValue: string;
    targetTable: string;
    targetColumn: string;
}
