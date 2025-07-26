# API Documentation

This document provides comprehensive documentation for all remote APIs used by the NotifyForwarders Android application.

## Overview

The NotifyForwarders app communicates with a remote server using HTTP REST APIs to forward notifications and send content. All endpoints use JSON format for data exchange and require a configured server address.

## Base Configuration

- **Protocol**: HTTP
- **Default Port**: 19283 (automatically appended if not specified in server address)
- **Content-Type**: `application/json` (for all requests)
- **Character Encoding**: UTF-8
- **Base URL Format**: `http://[server_address]:[port]`

## Timeout Settings

| Endpoint | Connection Timeout | Read Timeout |
|----------|-------------------|--------------|
| `/api/notify` | 5 seconds | 5 seconds |
| `/api/clipboard/text` | 10 seconds | 10 seconds |
| `/api/clipboard/image` | 10 seconds | 10 seconds |
| `/api/image/raw` | 10 seconds | 10 seconds |

## Authentication

Currently, no authentication is required for any endpoints. The server identifies devices by the `devicename` field included in all requests.

## Common Request Fields

All API requests include the following common fields:

- **`devicename`**: String - Automatically detected Android device name (e.g., "Samsung Galaxy S21")

## API Endpoints

### 1. POST /api/notify

**Purpose**: Forward captured Android notifications to the server

**URL**: `http://[server_address]:[port]/api/notify`

**Method**: POST

**Content-Type**: application/json

**Request Body**:
```json
{
  "appname": "string",           // Application name that sent the notification
  "title": "string",             // Notification title
  "description": "string",       // Notification content/text
  "devicename": "string",        // Android device name
  "uniqueId": "string",          // Unique identifier (packageName:notificationId)
  "id": "number",                // Internal notification record ID
  "iconMd5": "string",           // MD5 hash of app icon (optional)
  "iconBase64": "string"         // Base64 encoded app icon data (optional)
}
```

**Request Example**:
```json
{
  "appname": "WhatsApp",
  "title": "John Doe",
  "description": "Hello, how are you?",
  "devicename": "Samsung Galaxy S21",
  "uniqueId": "com.whatsapp:12345",
  "id": 1001,
  "iconMd5": "a1b2c3d4e5f6...",
  "iconBase64": "iVBORw0KGgoAAAANSUhEUgAA..."
}
```

**Response**:
- **200 OK**: Notification successfully received and processed
- **4xx/5xx**: Error occurred (specific error handling depends on server implementation)

**Notes**:
- Icon fields (`iconMd5`, `iconBase64`) are only included when notification icon feature is enabled
- The `uniqueId` field helps prevent duplicate notifications
- Notifications with empty title and description are filtered out

---

### 2. POST /api/clipboard/text

**Purpose**: Send clipboard text content to the server

**URL**: `http://[server_address]:[port]/api/clipboard/text`

**Method**: POST

**Content-Type**: application/json

**Request Body**:
```json
{
  "content": "string",           // Base64 encoded text content
  "devicename": "string",        // Android device name
  "type": "text"                 // Content type identifier
}
```

**Request Example**:
```json
{
  "content": "SGVsbG8gV29ybGQh",
  "devicename": "Samsung Galaxy S21",
  "type": "text"
}
```

**Response**:
- **200 OK**: Text content successfully received
- **4xx/5xx**: Error occurred

**Notes**:
- Text content is Base64 encoded to handle special characters and encoding issues
- Triggered by "Send Clipboard" action when clipboard contains text

---

### 3. POST /api/clipboard/image

**Purpose**: Send clipboard image content to the server

**URL**: `http://[server_address]:[port]/api/clipboard/image`

**Method**: POST

**Content-Type**: application/json

**Request Body**:
```json
{
  "content": "string",           // Base64 encoded image data
  "devicename": "string",        // Android device name
  "type": "image"                // Content type identifier
}
```

**Request Example**:
```json
{
  "content": "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBD...",
  "devicename": "Samsung Galaxy S21",
  "type": "image"
}
```

**Response**:
- **200 OK**: Image content successfully received
- **4xx/5xx**: Error occurred

**Notes**:
- Image data is Base64 encoded
- Triggered by "Send Clipboard" action when clipboard contains an image URI
- Supports various image formats (JPEG, PNG, etc.)

---

### 4. POST /api/image/raw

**Purpose**: Send gallery images with EXIF metadata to the server

**URL**: `http://[server_address]:[port]/api/image/raw`

**Method**: POST

**Content-Type**: application/json

**Headers**:
- **`X-EXIF`**: Base64 encoded EXIF metadata JSON string (optional)

**Request Body**:
```json
{
  "content": "string",           // Base64 encoded image data
  "devicename": "string",        // Android device name
  "mimeType": "string"           // Image MIME type (e.g., "image/jpeg")
}
```

**Request Example**:
```json
{
  "content": "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBD...",
  "devicename": "Samsung Galaxy S21",
  "mimeType": "image/jpeg"
}
```

**Headers Example**:
```
X-EXIF: eyJEYXRlVGltZSI6IjIwMjM6MTI6MzEgMTQ6MzA6MDAiLCJDYW1lcmEiOiJTYW1zdW5nIEdhbGF4eSBTMjEifQ==
```

**Response**:
- **200 OK**: Image successfully received
- **4xx/5xx**: Error occurred

**Notes**:
- Triggered by "Send Image" action to send the latest gallery image
- EXIF metadata is included in the `X-EXIF` header when available
- EXIF data contains camera information, GPS coordinates, timestamps, etc.

## Error Handling

### Client-Side Error Handling

The app implements comprehensive error handling:

1. **Server Configuration Validation**: Checks if server address is configured before making requests
2. **Network Connectivity**: Handles connection timeouts and network errors
3. **Permission Errors**: Manages clipboard and storage permission issues
4. **User Notifications**: Shows detailed error messages via Android notifications

### Common Error Scenarios

1. **Server Not Configured**: App shows error notification if no server address is set
2. **Connection Timeout**: Network requests timeout after specified duration
3. **Server Unreachable**: HTTP connection fails due to network issues
4. **Permission Denied**: Clipboard or storage access denied by user
5. **Invalid Content**: Empty or corrupted content cannot be processed

## Security Considerations

1. **No Authentication**: Currently no authentication mechanism is implemented
2. **HTTP Protocol**: Data is transmitted in plain text (consider HTTPS for production)
3. **Network Security**: App allows cleartext HTTP traffic for debugging purposes
4. **Data Privacy**: All content (text, images, notifications) is sent to the configured server

## Implementation Details

### Network Configuration

The app uses Android's `HttpURLConnection` for all HTTP requests with the following configuration:

```xml
<!-- Network Security Config -->
<network-security-config>
    <base-config cleartextTrafficPermitted="true">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

### Required Permissions

- **INTERNET**: Required for all network communication
- **POST_NOTIFICATIONS**: For displaying status notifications
- **READ_MEDIA_IMAGES**: For accessing gallery images (Android 13+)
- **READ_EXTERNAL_STORAGE**: For accessing images (Android 12 and below)

### Device Name Detection

The app automatically detects the device name using `Build.MODEL` and includes it in all API requests for device identification purposes.

## Testing and Verification

The app includes a server verification feature that sends a test notification to validate the server configuration:

**Test Request to `/api/notify`**:
```json
{
  "devicename": "Samsung Galaxy S21",
  "appname": "NotifyForwarders",
  "title": "Server Verification",
  "description": "Verification code: [random_code]"
}
```

This helps users confirm their server is properly configured and reachable.

## Summary

### Complete API Endpoint List

The NotifyForwarders Android application uses exactly **4 remote API endpoints**:

| Endpoint | Method | Purpose | Timeout |
|----------|--------|---------|---------|
| `/api/notify` | POST | Forward captured notifications | 5s |
| `/api/clipboard/text` | POST | Send clipboard text content | 10s |
| `/api/clipboard/image` | POST | Send clipboard image content | 10s |
| `/api/image/raw` | POST | Send gallery images with EXIF | 10s |

### Network Configuration Summary

- **Protocol**: HTTP only (cleartext traffic permitted)
- **Default Port**: 19283
- **Content-Type**: application/json (all endpoints)
- **Authentication**: None required
- **Device Identification**: Via `devicename` field in all requests
- **Data Encoding**: Base64 for all content (text, images, icons)

### Key Features

1. **Real-time Notification Forwarding**: Captures Android notifications and forwards them instantly
2. **Clipboard Integration**: Supports both text and image content from clipboard
3. **Gallery Image Sending**: Sends latest images with EXIF metadata preservation
4. **Icon Support**: Optional app icon forwarding with MD5 verification
5. **Error Handling**: Comprehensive error notifications and retry mechanisms
6. **Multi-language Support**: All API interactions support 7 languages

### Server Requirements

To implement a compatible server, you need to support:
- HTTP POST requests with JSON payloads
- Base64 decoding for content and images
- Optional EXIF metadata handling via custom headers
- Device identification via `devicename` field
- Standard HTTP response codes (200 for success)

This documentation covers all remote API interactions in the NotifyForwarders Android application.
