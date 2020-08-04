#include "coronatracker-crypto.h"

#define RPIK_INFO_LENGTH 7
#define RPIK_LENGTH 16

#define TEK_LENGTH 16

#define PADDED_DATA_LENGTH 16
#define PADDED_DATA_INFO_START 0
#define PADDED_DATA_ZEROS_START 6
#define PADDED_DATA_ENIN_START 12

#define RPI_LENGTH 16

signed char RPIK_INFO[7] = {69, 78, 45, 82, 80, 73, 75};   //"EN-RPIK"
signed char PADDED_DATA_INFO[] = {69, 78, 45, 82, 80, 73}; //"EN-RPI"

int calculateRollingProximityIdentifierKey(const unsigned char *tek, unsigned char *output)
{
    return mbedtls_hkdf_expand(mbedtls_md_info_from_type(MBEDTLS_MD_SHA256), tek, TEK_LENGTH, (const unsigned char *)RPIK_INFO, RPIK_INFO_LENGTH, output, RPIK_LENGTH);
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

    signed char RPIK[16];
    ret = calculateRollingProximityIdentifierKey(tek, (unsigned char *)RPIK);
    if (ret != 0)
    {
        return ret;
    }

    signed char PaddedData[16];
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