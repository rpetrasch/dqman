
export const IntegrationTypeOptions = ['RDBMS', 'NOSQL', 'Web', 'CSV', 'Text'];

export interface DqIntegration {
    id?: number;
    name: string;
    description: string;
    type: string;
    url: string;
    user?: string;
    password?: string;
}
