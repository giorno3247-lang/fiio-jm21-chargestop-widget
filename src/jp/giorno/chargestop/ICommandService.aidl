package jp.giorno.chargestop;

interface ICommandService {
    int setChargingDisable(int value) = 1;
    void destroy() = 16777114;
}
