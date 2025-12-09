
export const RuleTypeOptions = ['SQL', 'REGEX', 'Python', 'Java', 'JavaScript', 'Groovy', 'AI (LLM)', 'ML', 'ADVANCED'];

export interface DqRule {
    id?: number;
    name: string;
    description: string;
    ruleType: string;
    ruleValue: string;
    sourceTableFieldList?: string[];
}
