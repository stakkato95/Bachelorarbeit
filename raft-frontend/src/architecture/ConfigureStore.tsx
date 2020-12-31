import { createStore, combineReducers, applyMiddleware } from 'redux';
import { reducer } from './RootReducer';

import createSagaMiddleware from 'redux-saga';

import { rootSaga } from './RootSaga';

export const ConfigureStore = () => {
    const sagaMiddleware = createSagaMiddleware();

    const store = createStore(reducer, applyMiddleware(sagaMiddleware));
    sagaMiddleware.run(rootSaga);

    return store;
};