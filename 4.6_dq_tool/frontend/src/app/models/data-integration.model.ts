
export const IntegrationTypeOptions = ['RDBMS', 'NOSQL', 'Web', 'CSV', 'Text'];

export interface DataIntegration {
    id?: number;
    name: string;
    description: string;
    type: string;
    url: string;
    user?: string;
    password?: string;
}
