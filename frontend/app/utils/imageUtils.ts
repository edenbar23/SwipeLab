/**
 * Parses an image URL or raw base64 string to a format suitable for the React Native Image component
 * or the AuthenticatedImage component.
 * 
 * @param url The image URL or raw base64 string
 * @returns A safe URI string to be used as an image source, or undefined if the input is empty
 */
export function parseImageUrl(url?: string): string | undefined {
    if (!url) return undefined;

    // Already a valid standard URL or Data URI
    if (url.startsWith('http') || url.startsWith('data:image') || url.startsWith('blob:') || url.startsWith('file:')) {
        return url;
    }

    // Relative API endpoints from the SwipeLab backend
    if (url.startsWith('/api/') || url.startsWith('/images/')) {
        return url;
    }

    // Otherwise, treat as a raw base64 string
    return `data:image/jpeg;base64,${url}`;
}
