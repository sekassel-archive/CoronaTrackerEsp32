#include "coronatracker-crypto.h"

#define RPIK_INFO_LENGTH 7
#define RPIK_LENGTH 16

#define RPI_LENGTH 16

#define AEMK_INFO_LENGTH 7
#define AEMK_LENGTH 16

#define METADATA_LENGTH 4

#define PADDED_DATA_LENGTH 16
#define PADDED_DATA_INFO_START 0
#define PADDED_DATA_ZEROS_START 6
#define PADDED_DATA_ENIN_START 12

#define PAYLOAD_LENGTH 20
#define PAYLOAD_METADATA_START 16

signed char RPIK_INFO[RPIK_INFO_LENGTH] ={ 69, 78, 45, 82, 80, 73, 75 }; //"EN-RPIK"
signed char AEMK_INFO[AEMK_INFO_LENGTH] ={ 69, 78, 45, 65, 69, 77, 75 }; //"EN-AEMK"
signed char PADDED_DATA_INFO[] ={ 69, 78, 45, 82, 80, 73 }; //"EN-RPI"

int calculateRollingProximityIdentifierKey(const unsigned char *tek, unsigned char *output)
{
    return mbedtls_hkdf_expand(mbedtls_md_info_from_type(MBEDTLS_MD_SHA256), tek, TEK_LENGTH, (const unsigned char *)RPIK_INFO, RPIK_INFO_LENGTH, output, RPIK_LENGTH);
}

int calculateAssociatedEncryptedMetadateKey(const unsigned char *tek, unsigned char* output) {
    return mbedtls_hkdf_expand(mbedtls_md_info_from_type(MBEDTLS_MD_SHA256), tek, TEK_LENGTH, (const unsigned char *)AEMK_INFO, AEMK_INFO_LENGTH, output, AEMK_LENGTH);
}

int calculateENIntervalNumber(long timestamp) {
    return (int)(timestamp/(60*10));
}

int calculateENIntervalNumber(uint32_t ENIN, unsigned char *output)
{
    output[0] = ENIN & 0x000000ff;
    output[1] = (ENIN & 0x0000ff00) >> 8;
    output[2] = (ENIN & 0x00ff0000) >> 16;
    output[3] = (ENIN & 0xff000000) >> 24;

    return 0;
}

int calculatePaddedData(uint32_t ENIntervalNumber, unsigned char *output)
{
    for (int i = PADDED_DATA_INFO_START; i < PADDED_DATA_ZEROS_START; i++)
    {
        output[i] = PADDED_DATA_INFO[i];
    }

    for (int i = PADDED_DATA_ZEROS_START; i < PADDED_DATA_ENIN_START; i++)
    {
        output[i] = 0;
    }

    signed char ENIN[4];
    int ret = calculateENIntervalNumber(ENIntervalNumber, (unsigned char *)ENIN);
    if (ret != 0)
    {
        return ret;
    }

    for (int i = PADDED_DATA_ENIN_START, j = 0; i < PADDED_DATA_LENGTH; i++, j++)
    {
        output[i] = ENIN[j];
    }

    return 0;
}

int calculateRollingProximityIdentifier(const unsigned char *tek, int ENIN, unsigned char *output)
{
    int ret = 0;

    signed char RPIK[RPIK_LENGTH];
    ret = calculateRollingProximityIdentifierKey(tek, (unsigned char *)RPIK);
    if (ret != 0)
    {
        return ret;
    }

    signed char PaddedData[PADDED_DATA_LENGTH];
    ret = calculatePaddedData(ENIN, (unsigned char *)PaddedData);
    if (ret != 0)
    {
        return ret;
    }

    mbedtls_aes_context aes;
    mbedtls_aes_init(&aes);

    ret = mbedtls_aes_setkey_enc(&aes, (const unsigned char *)RPIK, RPIK_LENGTH * 8);
    if (ret != 0)
    {
        return ret;
    }

    ret = mbedtls_aes_crypt_ecb(&aes, MBEDTLS_AES_ENCRYPT, (const unsigned char *)PaddedData, output);
    if (ret != 0)
    {
        return ret;
    }

    mbedtls_aes_free(&aes);

    return ret;
}

int getBLEAddress(const unsigned char *seed, int seedLength, uint8_t *output, int outputLength)
{
    int err = mbedtls_hkdf_expand(mbedtls_md_info_from_type(MBEDTLS_MD_SHA256), seed, seedLength, NULL, 0, output, outputLength);

    /*
    Most significant bits set to one, as per Bluetooth Core Specification Version 5.2 | Vol.6, Part B 1.3.2.1
    While this is a static address and not a private one, advertising does not seem to work otherwise
    */
    output[0] |= 0xC0; //1100 0000

    return err;
}

int calculateMetadata(signed char version, signed char tpl, signed char* output) {
    output[0] = version;
    output[1] = tpl;
    output[2] = 0;
    output[3] = 0;

    //TODO check for inconsistency with bluetooth specification
    return 0;
}

int calculateAssociatedEncryptedMetadata(const unsigned char* tek, unsigned char* RPI, const unsigned char* Metadata, unsigned char* output) {
    int ret = 0;

    signed char AEMK[AEMK_LENGTH];
    ret = calculateAssociatedEncryptedMetadateKey(tek, (unsigned char*)AEMK);

    mbedtls_aes_context aes;
    mbedtls_aes_init(&aes);

    ret = mbedtls_aes_setkey_enc(&aes, (const unsigned char *)AEMK, AEMK_LENGTH * 8);
    if (ret != 0)
    {
        return ret;
    }

    ret = mbedtls_aes_crypt_cbc(&aes, MBEDTLS_AES_ENCRYPT, 16, RPI, Metadata, output);
    if (ret != 0)
    {
        return ret;
    }

    mbedtls_aes_free(&aes);

    return ret;
}

int getAdvertisingPayload(const unsigned char *tek, int ENIN, signed char version, signed char tpl, unsigned char* output) {
    int ret = 0;
    signed char RPI[RPIK_LENGTH];
    ret = calculateRollingProximityIdentifier(tek, ENIN, (unsigned char *)RPI);

    if (ret != 0) {
        return ret;
    }

    for (int i = 0; i < PAYLOAD_METADATA_START; i++)
    {
        output[i] = RPI[i];
    }

    signed char Metadata[METADATA_LENGTH];
    ret = calculateMetadata(version, tpl, Metadata);

    if (ret != 0) {
        return ret;
    }

    signed char AEM[METADATA_LENGTH];
    ret = calculateAssociatedEncryptedMetadata(tek, (unsigned char*)RPI, (const unsigned char*)Metadata, (unsigned char *)AEM);

    if (ret != 0) {
        return ret;
    }

    for (int i = 16, j = 0; i < PAYLOAD_LENGTH; i++, j++)
    {
        output[i] = AEM[j];
    }

    return 0;
}

void generateTemporaryExposureKey(signed char *output)
{
    for (int i = 0; i < TEK_LENGTH; i++)
    {
        output[i] = random(SCHAR_MIN, SCHAR_MAX);
    }
}