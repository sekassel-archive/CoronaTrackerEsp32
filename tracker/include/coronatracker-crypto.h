#include "Arduino.h"
#include "coronatracker-hkdf.h"
#include "mbedtls/md.h"
#include "mbedtls/aes.h"

int calculateRollingProximityIdentifierKey(const unsigned char *tek, unsigned char *output);
int calculateRollingProximityIdentifier(const unsigned char *tek, int ENIN, unsigned char *output);
int calculateENIntervalNumber(uint32_t ENIN, unsigned char *output);
int calculatePaddedData(uint32_t ENIntervalNumber, unsigned char *output);