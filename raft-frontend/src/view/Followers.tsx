import React from 'react';

import { createStyles, Theme, makeStyles } from '@material-ui/core/styles';

import Typography from '@material-ui/core/Typography';

import Follower from './Follower';

const useStyles = makeStyles((theme: Theme) =>
    createStyles({
        root: {
            width: '100%',
            maxWidth: 360,
            backgroundColor: theme.palette.background.paper,
        },
        nested: {
            paddingLeft: theme.spacing(4),
        },
        table: {
        },
    }),
);

export default function Followers(props: any) {
    const { followers } = props;
    const classes = useStyles();

    return (<>
        <Typography variant="h4" gutterBottom>Leader</Typography>
        {followers.map((follower: any, i: number) => (
            <>
                <Typography variant="h6" gutterBottom>Follower {i + 1}</Typography>
                <Follower follower={follower} />
            </>
        ))}
    </>);
}