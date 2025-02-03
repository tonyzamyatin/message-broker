package mb.broker.exchange;

import mb.enums.ExchangeType;

public class DefaultExchange extends DirectExchange {
    @Override
    public ExchangeType getType() {
        return ExchangeType.DEFAULT;
    }
}
