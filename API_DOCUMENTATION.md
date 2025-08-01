# API Documentation

This document provides comprehensive documentation for all remote APIs used by the NotifyForwarders Android application.

## Overview

The NotifyForwarders app communicates with a remote server using HTTP REST APIs to forward notifications and send content. All endpoints use JSON format for data exchange and require a configured server address.

All API constants, endpoints, timeouts, and field names are centrally managed in the `ApiConstants.kt` file for consistency and maintainability.

## Base Configuration

- **Protocol**: HTTP (with HTTPS support)
- **Default Port**: 19283 (automatically appended if not specified in server address)
- **Content-Type**: `application/json` (for all requests)
- **Character Encoding**: UTF-8
- **Base URL Format**: `http://[server_address]:[port]`

## Constants File Reference

All API-related constants are defined in:
```
app/src/main/java/com/hestudio/notifyforwarders/constants/ApiConstants.kt
```

This file contains:
- **Base Configuration**: Protocols, ports, content types, encoding
- **API Endpoints**: All endpoint paths
- **HTTP Methods**: GET and POST method constants
- **Timeout Settings**: Connection and read timeouts for each endpoint type
- **JSON Field Names**: Standardized field names for all API requests
- **HTTP Headers**: Custom headers (if any)
- **Content Types**: Text and image content type identifiers
- **Utility Methods**: URL building and server address formatting

## Timeout Settings

| Endpoint | Connection Timeout | Read Timeout |
|----------|-------------------|--------------|
| `/api/notify` | 5 seconds | 5 seconds |
| `/api/notify/clipboard/text` | 10 seconds | 10 seconds |
| `/api/notify/clipboard/image` | 10 seconds | 10 seconds |
| `/api/notify/image/raw` | 10 seconds | 10 seconds |
| `/api/version` | 5 seconds | 5 seconds |

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

### 2. POST /api/notify/clipboard/text

**Purpose**: Send clipboard text content to the server

**URL**: `http://[server_address]:[port]/api/notify/clipboard/text`

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

### 3. POST /api/notify/clipboard/image

**Purpose**: Send clipboard image content to the server

**URL**: `http://[server_address]:[port]/api/notify/clipboard/image`

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

### 4. POST /api/notify/image/raw

**Purpose**: Send gallery images with file metadata to the server

**URL**: `http://[server_address]:[port]/api/notify/image/raw`

**Method**: POST

**Content-Type**: application/json

**Request Body**:
```json
{
  "content": "string",           // Base64 encoded image data
  "devicename": "string",        // Android device name
  "mimeType": "string",          // Image MIME type (e.g., "image/jpeg")
  "fileName": "string",          // Original filename (optional)
  "filePath": "string",          // Full file path (optional)
  "dateAdded": "number",         // Creation timestamp in seconds (optional)
  "dateModified": "number"       // Modification timestamp in seconds (optional)
}
```

**Request Example**:
```json
{
  "content": "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBD...",
  "devicename": "Samsung Galaxy S21",
  "mimeType": "image/jpeg",
  "fileName": "IMG_20231231_143000.jpg",
  "filePath": "/storage/emulated/0/DCIM/Camera/IMG_20231231_143000.jpg",
  "dateAdded": 1704024600,
  "dateModified": 1704024600
}
```

**Response**:
- **200 OK**: Image successfully received
- **4xx/5xx**: Error occurred

**Notes**:
- Triggered by "Send Image" action to send the latest gallery image
- File metadata includes filename, creation time, modification time, and file path
- Timestamps are provided in Unix timestamp format (seconds since epoch)
- All metadata fields are optional and may not be available for all images

---

### 5. GET /api/version

**Purpose**: Check server version compatibility

**URL**: `http://[server_address]:[port]/api/version`

**Method**: GET

**Content-Type**: Not applicable (GET request)

**Request Body**: None

**Response**:
```json
{
  "version": "string"           // Server version string (e.g., "1.0.1")
}
```

**Response Example**:
```json
{
  "version": "1.0.1"
}
```

**Response Codes**:
- **200 OK**: Version information successfully retrieved
- **4xx/5xx**: Error occurred or server doesn't support version checking

**Notes**:
- Used during server configuration to verify compatibility
- Required server version is defined in app strings as `server_version_required`
- If server version doesn't match, user receives compatibility warning
- This endpoint helps ensure the app works correctly with the configured server

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

This helps users confirm their server is properly configured and reachable. The app name constant `ApiConstants.APP_NAME` is used for the `appname` field in verification requests.

## Summary

### Complete API Endpoint List

The NotifyForwarders Android application uses exactly **5 remote API endpoints**:

| Endpoint | Method | Purpose | Timeout |
|----------|--------|---------|---------|
| `/api/notify` | POST | Forward captured notifications | 5s |
| `/api/notify/clipboard/text` | POST | Send clipboard text content | 10s |
| `/api/notify/clipboard/image` | POST | Send clipboard image content | 10s |
| `/api/notify/image/raw` | POST | Send gallery images with metadata | 10s |
| `/api/version` | GET | Check server version compatibility | 5s |

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
3. **Gallery Image Sending**: Sends latest images with file metadata (filename, timestamps, file path)
4. **Icon Support**: Optional app icon forwarding with MD5 verification
5. **Error Handling**: Comprehensive error notifications and retry mechanisms
6. **Multi-language Support**: All API interactions support 7 languages

### Server Requirements

To implement a compatible server, you need to support:
- HTTP POST requests with JSON payloads for data endpoints
- HTTP GET requests for version checking
- Base64 decoding for content and images
- Optional file metadata handling in JSON payload
- Device identification via `devicename` field
- Version compatibility checking via `/api/version` endpoint
- Standard HTTP response codes (200 for success)

### Constants Reference

All API constants used in this documentation are defined in:
```
app/src/main/java/com/hestudio/notifyforwarders/constants/ApiConstants.kt
```

Key constant categories:
- **Endpoints**: `ENDPOINT_NOTIFY`, `ENDPOINT_CLIPBOARD_TEXT`, `ENDPOINT_CLIPBOARD_IMAGE`, `ENDPOINT_IMAGE_RAW`, `ENDPOINT_VERSION`
- **Timeouts**: `TIMEOUT_*_CONNECT` and `TIMEOUT_*_READ` for each endpoint type
- **Field Names**: `FIELD_DEVICE_NAME`, `FIELD_APP_NAME`, `FIELD_CONTENT`, etc.
- **Headers**: Custom headers (if any)
- **Content Types**: `CONTENT_TYPE_TEXT`, `CONTENT_TYPE_IMAGE`
- **Utility Methods**: `buildApiUrl()`, `formatServerAddress()`

This documentation covers all remote API interactions in the NotifyForwarders Android application.
