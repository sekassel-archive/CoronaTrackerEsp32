#include "Arduino.h"
#include "coronatracker-hkdf.h"
#include "mbedtls/md.h"
#include "mbedtls/aes.h"

#define EKROLLING_PERIOD 144
#define TEK_LENGTH 16

int calculateRollingProximityIdentifierKey(const unsigned char *tek, unsigned char *output);
int calculateRollingProximityIdentifier(const unsigned char *tek, int ENIN, unsigned char *output);
int calculateENIntervalNumber(long timestamp);
int calculateENIntervalNumber(uint32_t ENIN, unsigned char *output);
int calculatePaddedData(uint32_t ENIntervalNumber, unsigned char *output);
int calculateMetadata(signed char version, signed char tpl, signed char* output);
int calculateAssociatedEncryptedMetadata(const unsigned char* tek, unsigned char* RPI, const unsigned char* Metadata, unsigned char* output);
int getAdvertisingPayload(const unsigned char *tek, int ENIN, signed char version, signed char tpl, unsigned char* output);
void generateTemporaryExposureKey(signed char *output);
int getBLEAddress(const unsigned char* seed, int seedLength, uint8_t *output, int outputLength);