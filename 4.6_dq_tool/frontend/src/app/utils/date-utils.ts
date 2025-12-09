/**
 * Utility functions for the DQ Tool application
 */

/**
 * Formats a date string to YYYY-MM-DD HH:mm:ss format
 * Handles dates from the backend that may include timezone names in brackets
 * @param dateStr - The date string to format
 * @returns Formatted date string or 'N/A' if invalid
 */
export function formatDateTime(dateStr: string): string {
    if (!dateStr) return 'N/A';

    try {
        // Remove timezone name in brackets (from backend)
        const cleanedDateStr = dateStr.replace(/\[.*?\]$/, '');
        const date = new Date(cleanedDateStr);

        if (isNaN(date.getTime())) {
            console.warn('Invalid date, returning original string:', dateStr);
            return dateStr; // Return original string if invalid
        }

        // Format as YYYY-MM-DD HH:mm:ss
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        const seconds = String(date.getSeconds()).padStart(2, '0');

        return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
    } catch (e) {
        console.error('Error formatting date:', e);
        return dateStr;
    }
}
