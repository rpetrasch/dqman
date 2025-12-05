
export const RuleTypeOptions = ['SQL', 'REGEX', 'AI (LLM)', 'ML', 'ADVANCED'];

export interface DqRule {
    id?: number;
    name: string;
    description: string;
    ruleType: string;
    ruleValue: string;
    targetTable: string;
    targetColumn: string;
}
